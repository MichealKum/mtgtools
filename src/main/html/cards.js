var cards = (function() {
    var storeCards = "registered.cards";
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
        var str = "Name\tCount\tset_code\n";
        for (name in registered) {
            var sets = registered[name];
            var count = sets.length;
            if (count == 0) {
                continue;
            }
            var regSet = {};
            for (idx in sets) {
                var setCode = sets[idx];
                if (!regSet[setCode]) {
                    regSet[setCode] = 0;
                }
                regSet[setCode] = regSet[setCode] + 1;
            }
            for (setCode in regSet) {
                str += name + "\t" + regSet[setCode] + "\t" + setCode  + "\n";
            }
        }
        $("#interface").val(str);
    }

    function keyPressed(event) {
        window.data = event;
        $(event.currentTarget).val("");
        var exp = $("#expansion option:selected");
        var max = Number.parseInt(exp.attr("data-max"));
        var eCode = event.originalEvent.key;
        if (eCode <= "9" && eCode >= "0") {
            current.typed = (current.typed * 10 + Number.parseInt(eCode)) % 1000;
            console.log("typed: " + current.typed);
            if (current.typed > 0 && current.typed <= max) {
                current.number = current.typed;
                showCurrent();
            }
            return false;
        }
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
                regValue.push(current.exp);
                save();
                showCurrent();
                break;
            case "ArrowLeft":
                var index = regValue.indexOf(current.exp);
                if (index >= 0) {
                    var tail = regValue.splice(index);
                    tail.pop();
                    regValue = regValue.concat(tail);
                    save();
                    showCurrent();
                }
                break;
        }
        return false;
    };

    function showCurrent() {
        current.exp = $("#expansion").val();
        current.card = cardsByNum[current.exp + "_" + current.number];
        window.card = current.card;
        regValue = registered[current.card.info[0].card.name];
        if (!regValue) {
            regValue = [];
        }
        var expCount = 0;
        for (idx in regValue) {
            if (regValue[idx] == current.exp) {
                expCount++;
            }
        }
        var text = current.number + "." + current.card.info[0].card.name;
        var rus = byLanguage(current.card, "Russian");
        if (rus) {
            text += " (" + rus.card[0].name + ") ";
        }
        $(".face_one .card_name").text(text);
        $(".face_one .card_face").attr("src", current.card.info[0].card.image);
        $("#cards_count").text(expCount + "(" + regValue.length + ")");
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
        init:       init,
        addCards:   addCards
    };
    return model;
})();
