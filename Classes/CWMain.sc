ClockWise {
    var <defaultNode, points, midiInIds, midiOuts;

    *new { |defaultNode=nil|
        ^super.newCopyArgs(defaultNode,
            Dictionary(), Dictionary(), Dictionary())
    }
    free {
        var ps = points;
        points = nil;
        ps.do(_.free);
        super.free;
    }

    cmdPeriod { this.free; }

    midiDevice { |devName, devSearch, portSearch=nil, inOnly=false, outOnly=false|
        var find = { | dir, list, action |
            var found = list.detectIndex { |ep, i|
                ep.device.containsi(devSearch)
                && (portSearch !? ep.name.containsi(_) ? true)
            };
            if (found.isNil) {
                "midiDevice %/% no match found".format(devName, dir).postln;
            } {
                var ep = list.at(found);
                action.value(ep, found);
                "midiDevice %/% is %".format(devName, dir, ep).postln;
            };
        };

        if (MIDIClient.initialized.not) {
			MIDIClient.init();
		};

        if (outOnly.not) {
            find.("in ", MIDIClient.sources)
            { |ep, i|
                try {
					MIDIIn.connect(i, ep)
				} { |err|
					if (err.isKindOf(PrimitiveFailedError)) 
						{ "You can ignore that error message.".postln; }
						{ err.throw; };
				};
                midiInIds.put(devName, ep.uid);
            };
        };
        if (inOnly.not) {
            find.("out", MIDIClient.destinations)
            { |ep, i|
                var m = MIDIOut(i, ep.uid);
                m.latency = 0;
				try {
					m.connect(ep.uid);
				} { |err|
					if (err.isKindOf(PrimitiveFailedError)) 
						{ "You can ignore that error message.".postln; }
						{ err.throw; };
				};
                midiOuts.put(devName, m);
            };
        };
    }

    point { |symbol|
        ^points.atFail(symbol) {
            var p = CWPoint();
            points.put(symbol, p);
            p
        }
    }

    getMidiIn  { |dev, skip=false| ^if (skip, nil, { midiInIds.at(dev) }) }
    getMidiOut { |dev, skip=false| ^if (skip, nil, {  midiOuts.at(dev) }) }


    synthArg { |pt, spec = nil, symbol = nil, node = nil|
        CWSynthControl(
            this.point(pt),
            node ? this.defaultNode,
            symbol ? pt,
            spec);
    }

    midiCC { |pt, dev, ch, cc, spec=nil, unmapped=false, inOnly=false, outOnly=false|
        CWCc(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch, cc, spec, unmapped);
    }

    midiBend { |pt, dev, ch, spec=nil, unmapped=false, inOnly=false, outOnly=false|
        CWBend(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch, spec, unmapped);
    }

    midiEncoder { |pt, dev, ch, cc, increment, mode=\universal, inOnly=false, outOnly=false|
        CWEncoder(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch, cc, increment, mode);
    }

    midiRadioButton { |pt, value, dev, ch, note=nil, cc=nil, inOnly=false, outOnly=false|
        CWRadioButton(
            this.point(pt),
            value,
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch, note, cc);
    }

    midiTriggerButton { |pt, dev, ch, note=nil, cc=nil, inOnly=false, outOnly=false|
        CWTriggerButton(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch, note, cc);
    }

    midiPerf { |pt, dev, ch=nil, inOnly=false, outOnly=false|
        CWPerf(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch);
    }

    synthPerf { |pt, synth|
        var s = CWSynthPerf(
            this.point(pt),
            synth);
        ^s.group;
    }

    midiProgram { |pt, dev, ch, inOnly=false, outOnly=false|
        CWProgram(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly),
            ch);
    }

    tempoClock { |tempoPt, clockPt, clock|
        CWTempoClock(
            this.point(tempoPt),
            this.point(clockPt),
            clock);
    }

    midiClock { |pt, dev, inOnly=false, outOnly=false|
        CWMidiClock(
            this.point(pt),
            this.getMidiIn(dev, skip:outOnly),
            this.getMidiOut(dev, skip:inOnly)
        );
    }

    select { |commonPt, selectorPt, muxPts, sync = true|
        CWSelect(
            this.point(commonPt),
            this.point(selectorPt),
            muxPts.collect ( this.point(_) ),
            sync
        );
    }

    warp { |basePt, warpPt, spec=nil, mul=1, add=0|
        CWWarp(
            this.point(basePt),
            this.point(warpPt),
            spec, mul, add);
    }

    action { |pt, a| CWAction(this.point(pt), a);  }
    saneValue { |pt, v| CWSane(this.point(pt), v); }

    triggerSane { |trigSym, destSyms|
        var dests = destSyms.collect (this.point(_));
        CWAction(
            this.point(trigSym),
            { dests.do (_.sane); }
        );
    }


    set { |pt, nv| points.at(pt) !? (_.distribute(\set, nv)) }

    sane { |pt| points.at(pt) !? (_.sane) }
    saneAll { points.do (_.sane) }

    sync { |pt| points.at(pt) !? (_.sync) }
    syncAll { points.do (_.sync) }

}
