CWSynthControl : CWControl {
    var node, symbol, spec, saneValue, lastValue;

    *new { |point, node, symbol, spec=nil |
        ^super.new().initSynth(point, node, symbol, spec);
    }
    initSynth { |point, node_, symbol_, spec_|
        node = node_;
        symbol = symbol_;
        spec = spec_;
        saneValue = 0;
        lastValue = 0;

        node.get(symbol, { |cv|
            saneValue = spec !? _.unmap(cv) ? cv;
            lastValue = saneValue;
        });

        this.connect(point);
    }
    free {
        node = nil;
        super.free;
    }

    set { |v|
        lastValue = v;
        node.set(symbol, spec !? _.map(v) ? v);
    }

    sane {
        this.set(saneValue);
        this.send(\set, saneValue);
    }
    sync { this.send(\set, lastValue); }

}
