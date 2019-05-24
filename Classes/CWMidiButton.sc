// BUTTONS

CWButtonBase : CWControl {
    // Decodes note on and note off messages for a particular note.
    // Subclasses are expected to then do something with this.

    var midiFuncs, midiSend;

    *new { |point, devId, midiOut, ch, note=nil cc=nil |
        ^super.new().initNoteControl(point, devId, midiOut, ch, note, cc)
    }
    initNoteControl { |point, devId, midiOut, ch, note=nil, cc=nil|
        if (note.isNil == cc.isNil) {
            if (note.isNil)
                { Error("Must specify note or cc").throw; }
                { Error("Can't specify both not and cc").throw; };
        };

        midiFuncs = [];
        if (devId.isNil.not) {
            if (note.isNil.not) {
                midiFuncs.add(MIDIFunc.noteOn(this.decodeValue(_), note, ch, devId));
                midiFuncs.add(MIDIFunc.noteOff(this.buttonOff(_), note, ch, devId));
            };
            if (cc.isNil.not) {
                midiFuncs.add(MIDIFunc.cc(this.decodeValue(_), cc, ch, devId));
            };
        };
        if (midiOut.isNil.not) {
            if (note.isNil.not) {
                midiSend = { |vel|
                    if (vel > 0)
                        { midiOut.noteOn(ch, note, vel); }
                        { midiOut.noteOff(ch, note, vel); }
                };
            };
            if (cc.isNil.not) {
                midiSend = { |val|
                    midiOut.control(ch, cc, val);
                };
            };
        };

        this.connect(point);
    }
    free {
        midiFuncs.do (_.free);
        midiFuncs = nil;
        midiSend = nil;
        super.free;
    }

    decodeValue { |v|
        if (v > 0)
            { this.buttonOn() }
            { this.buttonOff() }
    }

    buttonOn { this.subclassResponsibility(thisMethod); }
    buttonOff { this.subclassResponsibility(thisMethod); }

    outputButton { |on| midiSend !? _.(if (on, 127, 0)); }
}

CWRadioButton : CWButtonBase {
    // Interprets a particular MIDI note as button to turn on.

    // The button is set on when a velocity > 1 is received.
    // The state of the controll will be reflected back as a vel 0 or 127.
    // If toggle is provided, then hitting the radio button again when on,
	// will set the point to the toggle value.

    var value, orGreater, toggle, active;

    *new { |point, value, devId, midiOut, ch, note=nil, cc=nil, orGreater=false, toggle=nil|
        ^super.new(point, devId, midiOut, ch, note, cc)
            .initRadioButton(value, orGreater, toggle)
    }
    initRadioButton { |value_, orGreater_, toggle_|
        value = value_;
        orGreater = orGreater_;
		toggle = toggle_;
        active = false;
    }

    buttonOn {
		if (active && toggle.isNil.not)
			{ active = false; this.send(\set, toggle); }
			{ active = true;  this.send(\set, value); }
    }
    buttonOff {
        // Resend on so as to make sure the controller has the right state
        // because controllers will turn the indicator light off when the
        // button is released.
        if (active) {
            this.outputButton(true);
        }
    }

    set { |v|
        active = if(orGreater, v >= value, v == value);
        this.outputButton(active);
    }
}

CWTriggerButton : CWButtonBase {
    // Interprets a particular MIDI note as a trigger.

    var triggerRoutine;

    *new { |point, devId, midiOut, ch, note=nil, cc=nil |
        ^super.new(point, devId, midiOut, ch, note, cc).initTriggerButton()
    }
    initTriggerButton {
        triggerRoutine = Routine({
                this.outputButton(true);
                0.150.yield;
                this.outputButton(false);
                nil
            });
    }
    buttonOn { this.send(\trigger); }
    buttonOff { }

    trigger { SystemClock.play(triggerRoutine.reset); }
}

