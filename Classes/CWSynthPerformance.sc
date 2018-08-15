CWSynthPerf : CWControl {
    // A Synth player.

    var synth, activeVoices;

    *new { |point, synth|
        ^super.new().initSynthPerf(point, synth)
    }
    initSynthPerf { |point, synthArg|
        synth = synthArg;
        activeVoices = nil!128;

        this.connect(point);
    }
    free {
        this.allNotesOff;
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
                ]);
        } {
        }
    }

    //allNotesOff { (0,127).do(self.noteOff(_, 0)) }
}
