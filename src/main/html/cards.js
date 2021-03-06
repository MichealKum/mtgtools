var cards = (function() {
    var storeCards = "registeredCards";
    var cardsExps = [];
    var cardsSet = {};
    var expCards = {};
    var cardsByNum = {};
    var cardsByName = {};
    var regValue = {};
    var current = {number: 1, typed: 1, exp: {}, card: {}};
    var registered = localStorage.getItem(storeCards);
    if (registered) {
        registered = JSON.parse(registered);
    } else {
        registered = {};
    }

    function save() {
        registered[current.card.info[0].card.name] = regValue;
        localStorage.setItem(storeCards, JSON.stringify(registered));
    }

    function doExport() {
        var sorted = [];
        for (name in registered) {
            sorted.push(name);
        }
        sorted.sort();
        var str = "";
        for (idx in sorted) {
            var name = sorted[idx];
            var count = registered[name].length;
            if (count == 0) {
                continue;
            }
            var card = cardsByNum[cardsByName[name]];
            var rus = "";
            var rusLang = byLanguage(card, "Russian");
            if (rusLang) {
                rus = rusLang.name;
            }
            var cs = card.info[0].characteristics;
            str += name + "\t" + count + "\t" + rus + "\t" + cs.rarity + "\n";
        }
        $("#interface").val(str);
    }

    function createCSV() {
        var str = "Name\tCount\tset_code\tis_foil\n";
        for (name in registered) {
            var exps = registered[name];
            for (exp in exps) {
                var expValue = exps[exp];
                if (expValue.norm > 0) {
                    str += name + "\t" + expValue.norm + "\t" + exp + "\t\n";
                }
                if (expValue.foil > 0) {
                    str += name + "\t" + expValue.foil + "\t" + exp + "\t1\n";
                }
            }
        }
        $("#interface").val(str);
    }

    function createExpValue() {
        return {norm: 0, foil: 0, total: 0};
    }

    function registerCard(exp, count, foil) {
        var expValue = regValue[exp];
        if (!expValue) {
            if (count < 0) {
                return;
            }
            expValue = createExpValue();
        }
        if (foil) {
            if (count < 0) {
                count = Math.max(-expValue.foil, count);
            }
            expValue.foil = expValue.foil + count;
        } else {
            if (count < 0) {
                count = Math.max(-expValue.norm, count);
            }
            expValue.norm = expValue.norm + count;
        }
        expValue.total = expValue.total + count;
        regValue[exp] = expValue;
        save();
        showCurrent();
    }

    function keyPressed(event) {
        window.data = event;
        $(event.currentTarget).val("");
        var exp = $("#expansion option:selected");
        var max = Number.parseInt(exp.attr("data-max"));
        var eCode = event.originalEvent.key;
        if (eCode <= "9" && eCode >= "0") {
            current.typed = (current.typed * 10 + Number.parseInt(eCode)) % 1000;
            if (current.typed > 0 && current.typed <= max) {
                current.number = current.typed;
                showCurrent();
            }
            return false;
        }
        var shiftKey = event.originalEvent.shiftKey;
        var ctrlKey = event.originalEvent.ctrlKey;
        switch (eCode) {
            case "e":
            case "E":
                doExport();
                break;
            case "c":
            case "C":
                createCSV();
                break;
            case "ArrowDown":
                if (current.number < max) {
                    current.number ++;
                    current.typed = current.number;
                    showCurrent();
                }
                break;
            case "ArrowUp":
                if (current.number > 1) {
                    current.number --;
                    current.typed = current.number;
                    showCurrent();
                }
                break;
            case "ArrowRight":
                registerCard(current.exp, 1, ctrlKey || shiftKey);
                break;
            case "ArrowLeft":
                registerCard(current.exp, -1, ctrlKey || shiftKey);
                break;
        }
        return false;
    };

    function showCurrent() {
        current.exp = $("#expansion").val();
        current.card = cardsByNum[current.exp + "_" + current.number];
        window.card = current.card;
        var cardName = current.card.info[0].card.name;

        var text = current.number + "." + cardName;
        var rus = byLanguage(current.card, "Russian");
        if (rus) {
            text += " (" + rus.card[0].name + ") ";
        }
        $(".face_one .card_name").text(text);
        $(".face_one .card_face").attr("src", current.card.info[0].card.image);


        var allSets = cardsByNum[cardsByName[cardName]].info[0].characteristics.allSets;
        regValue = registered[cardName];
        if (!regValue) {
            regValue = {};
        }

        var total = 0;
        $("#cards_per_exepnsion").text("");
        for (idx in allSets) {
            var sn = allSets[idx].shortName;
            var cnt = createExpValue();
            if (regValue[sn]) {
                cnt = regValue[sn];
            }
            total = total + cnt.total;
            var span = $("<span class='exp_info'/>")
                .attr("title", allSets[idx].name)
                .text(sn + ": " + cnt.norm + "n + " + cnt.foil + "f");
            $("#cards_per_exepnsion").append(span);
        }
        var cur = createExpValue();
        if (regValue[current.exp]) {
            cur = regValue[current.exp];
        }

        $("#cards_count").text(cur.norm + "n + " + cur.foil  + "f " + "(" + total + ")");
    }

    function byLanguage(card, lang) {
        var langs = card.info[0].languages;
        for (langIdx in langs) {
            if (lang == langs[langIdx].language[0]) {
                return langs[langIdx];
            }
        }
    }

    function changeExp() {
        current.number = 1;
        current.typed = 1;
        showCurrent();
    }

    function init() {
        var expSelect = $("#expansion");
        expSelect.find("option").remove();
        var exp = "";
        for (idx in cardsExps) {
            exp = cardsExps[idx];
            expSelect.prepend($("<option value='" + exp.shortName  + "' data-max='" + exp.max + "'>" + exp.name + "</option>"));
        }
        current.number = 1;
        $("#expansion").val(exp.shortName);
        $("#interface").val("");
        $("#typing").on("keypress", keyPressed);
        $("#expansion").on("change", changeExp);
        showCurrent();
    };

    function cmpExpansion(expansion) {
        var exp = expansion;
        var fn = function(obj) {
            return exp.name == obj.name &&
                exp.shortName == obj.shortName;
        };
        return fn;
    };

    function arrayIndex(arr, cmp) {
        for (idx in arr) {
            if (cmp(arr[idx])) {
                return idx;
            }
        }
        return -1;
    };

    function unite(name) {
        console.log("Not yet implemented: unite");
        return;
        var other = JSON.parse(localStorage.getItem(name));
        for (extName in other) {
            if (!registered[extName]) {
                registered[extName] = [];
            }
            registered[extName] = registered[extName].concat(other[extName]);
        }
        localStorage.setItem(storeCards, JSON.stringify(registered));
        showCurrent();
    }

    function addCards(newCards) {
            for (cardIdx in newCards) {
                var card = newCards[cardIdx];
                var chars = card.info[0].characteristics;
                var cardExp = chars.expansion;
                var idx = arrayIndex(cardsExps, cmpExpansion(cardExp));
                if (idx == -1) {
                    cardExp["max"] = chars.card.number;
                    cardsExps.push(cardExp);
                    expCards[cardExp.shortName] = [];
                    console.log("Add expansion " + cardExp.name);
                } else {
                    cardsExps[idx].max = Math.max(chars.card.number, cardsExps[idx].max);
                }
                var id = "id_" + card.id;
                cardsSet[id] = card;
                var fullName = cardExp.shortName + "_" + chars.card.number;
                cardsByNum[fullName] = card;
                cardsByName[card.info[0].card.name] = fullName;
                expCards[cardExp.shortName].push(id);
            }
    }

    var model = {
        init:     init,
        addCards: addCards,
        unite:    unite
    };
    return model;
})();
