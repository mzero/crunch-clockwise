CWSelect : CWControl {
    // Selects one of several points to join with another point, based on the
    // value of yet a third point!

    var selection;

    *new { | commonPoint, selectorPoint, muxPoints |
        ^super.new().initSelect(commonPoint, selectorPoint, muxPoints)
    }
    initSelect { | commonPoint, selectorPoint, muxPoints |
        this.connect(commonPoint, \common);
        this.connect(selectorPoint);
        muxPoints.do { |p, i|
            this.connect(p, i);
        };
        this.set(0);
    }

    receiveId { |id, msg, args|
        case
        { id == \common } { this.sendTo(selection, msg, *args); }
        { id == selection } { this.sendTo(\common, msg, *args); }
        { id.isNil } { this.receive(msg, args); }
    }

    set { |s|
        if (selection != s) {
            selection = s;
            points.at(s) !? _.sync();
        };
    }
}


CWSane : CWControl {
    // Supplies a sane value, and keeps track of the last value, so can be
    // re-sync'd.

    var <saneValue, <lastValue;

    *new { | point, value |
        ^super.new().initSane(point, value)
    }
    initSane { | p, v |
        saneValue = v;
        lastValue = v;
        this.connect(p);
    }

    set { |v|
        lastValue = v;
    }

    sane {
        this.set(saneValue);
        this.send(\set, saneValue);
    }
    sync {
        this.send(\set, lastValue);
    }
}


CWAction : CWControl {
    // Performs some action when set or triggered.

    var action;

    *new { | point, action |
        ^super.new().initAction(point, action)
    }
    initAction { | p, a |
        action = a;
        this.connect(p);
    }

    set { |v| action !? _.(v); }
    trigger { action !? _.(); }
}


CWWarp : CWControl {
    // Warp between two points.

    // The warpping is given by a ControlSpec (defaults to \unipolar), and/or
    // with mul and add options. This warpping is applied without clipping.

    // Set values from the base point are mapped to the warp point.
    // Set values from the warp point are unmapped back to the base point.

    var warp;

    *new { |basePoint, warpPoint, spec=nil, mul=1, add=0|
        ^super.new.initWarp(basePoint, warpPoint, spec, mul, add)
    }
    initWarp { |basePoint, warpPoint, spec=nil, mul=1, add=0|
        if (spec.isNil) { spec = \unipolar.asSpec; };
        if (mul != 1)   { spec = spec.zoom(mul); };
        if (add != 0)   { spec = spec.shift(add); };

        warp = spec.warp;

        this.connect(basePoint, \base);
        this.connect(warpPoint, \warp);
    }

    receiveId { |id, msg, args|
        case
            { id === \base && msg === \set } { this.toWarp(*args); }
            { id === \warp && msg === \set } { this.toBase(*args); }
    }

    toWarp { |b| this.sendTo(\warp, \set, warp.map(b)); }
    toBase { |w| this.sendTo(\base, \set, warp.unmap(w)); }
}

