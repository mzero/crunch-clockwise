// PERFORMANCE DATA CONTROLS

CWPerf : CWControl {
    // A "stream" of performance data. Encompases MIDI notes, touch (channel
    // and poly), bend, and a few key controls. This data as a whole is treated
    // as the control.

    var midiFuncs, midiOut, midiCh;

    *new { |point, devId, midiOut, ch, ccs=nil |
        ^super.new().initPerf(point, devId, midiOut, ch, ccs )
    }
    initPerf { |point, devId, out, ch, ccs |
        if (devId.isNil) {
            midiFuncs = [];
        } {
            midiFuncs = [
                MIDIFunc.noteOff   ( { |vel, note| this.send(\noteOff, note, vel); }, nil, ch, devId ),
                MIDIFunc.noteOn    ( { |vel, note| this.send(\noteOn, note, vel); }, nil, ch, devId ),
                MIDIFunc.polytouch ( { |val, note| this.send(\polytouch, note, val); }, nil, ch, devId ),
                MIDIFunc.program   ( { |num|       this.send(\program, num); }, ch, devId ),
                MIDIFunc.touch     ( { |val|       this.send(\touch, val); }, ch, devId ),
                MIDIFunc.bend      ( { |val|       this.send(\bend, val); }, ch, devId ),
            ]
            ++ ((ccs ? [1, 7, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73]).do { |ctl|
                MIDIFunc.cc        ( { |val|       this.send(\control, ctl, val); }, ctl, ch, devId )
            });
        };
        if (out.isNil.not && ch.isNil.not) {
            midiOut = out;
            midiCh = ch;
        };

        this.connect(point);
    }
    free {
        midiFuncs.do (_.free);
        midiFuncs = nil;
        midiOut = nil;
        super.free;
    }

    noteOff   { |note, vel| midiOut !? _.noteOff(midiCh, note, vel); }
    noteOn    { |note, vel| midiOut !? _.noteOn(midiCh, note, vel); }
    polytouch { |note, val| midiOut !? _.polytouch(midiCh, note, val); }
    control   { |ctl, val|  midiOut !? _.control(midiCh, ctl, val); }
    program   { |num|       midiOut !? _.program(midiCh, num); }
    touch     { |val|       midiOut !? _.touch(midiCh, val); }
    bend      { |val|       midiOut !? _.bend(midiCh, val); }

    allNotesOff { midiOut !? _.allNotesOff(midiCh); }

    sane {
        this.allNotesOff;   // since send() will skip this as it is the sender
        this.send(\allNotesOff);
    }
}
