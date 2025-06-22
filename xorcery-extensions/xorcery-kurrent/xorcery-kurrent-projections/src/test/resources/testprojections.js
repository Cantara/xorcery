fromAll()
.when({
    $init: function() {
        return {
            count: 0
        };
    },
    $any: function(s, e) {
        s.count += 1;
    }
})
.outputState();
