CWSynthPerf : CWControl {
    // A Synth player.

    var synth, <group, activeVoices;

    *new { |point, synth|
        ^super.new().initSynthPerf(point, synth)
    }
    initSynthPerf { |point, synthArg|
        synth = synthArg;
        activeVoices = nil!128;
        group = Group();

        this.connect(point);
    }
    free {
        this.allNotesOff;
        group.deepFree;
        group.freeAll;
        group.free;
    }

    noteOff { |note, vel|
        var voice = activeVoices[note];
        activeVoices[note] = nil;
        voice.release;
    }
    noteOn { |note, vel|
        var voice = activeVoices[note];
        if (voice.isNil) {
            activeVoices[note] = Synth(synth, [
                \freq, note.midicps,
                \amp, vel/127.0
                ], group);
        } {
        }
    }

    allNotesOff { for(0, 127, this.noteOff(_, 0)) }
}


/*
Open Issues:

Polyphony
[] limited polyphony?
[] pre-allocated voices?
[] voice stealing?
[] portamento?
[] envelop retrigger modes (from zero, from current?)
[] voice stealing modes (oldest, lowest, highest, etc...)
[] duplicate note mode: single voice or multi voice


Args
[] how to map from points?
[] from the CC's that are part of Perf?
[] use synthArg on the group node?
[] group only forwards .set() values to currently playing nodes
    [] if we think of them as knobs on a patch, then needs to be remembered
       by synthPerf an re-issued to new notes
       [] unless, of course, synths are pre-allocated and always on

[] map MIDI to freq, vol, etc? or pass raw midi values, or both (Patterns seem
    to do this)

*/
