CWTempoClock : CWControl {
	classvar midiClockBeats;
	var clock, tempo, clockRoutine, playing;

	*initClass {
		midiClockBeats = 1/24;
	}
	*new { |tempoPoint, clockPoint, clock, startPlaying=true|
		^super.new().initTempoClock(tempoPoint, clockPoint, clock, startPlaying)
	}
	initTempoClock { |tempoPoint, clockPoint, clockArg, startPlaying|
		clock = clockArg;
		tempo = clock.tempo;

		clock.addDependant(this);

		this.connect(clockPoint, \clock);
		this.connect(tempoPoint, \tempo);

		clockRoutine = Routine({
			loop {
				this.sendTo(\clock, \midiClock);
				midiClockBeats.yield;
			}
		});
		if(startPlaying) { this.play } { this.pause }
	}
	free {
		clockRoutine.stop();
		clock.removeDependant(this);
	}
	play {
		playing = true;
		clockRoutine.play(clock, quant:1);
	}
	pause {
		playing = false;
		clockRoutine.stop;
	}

	update { |obj, what ... args|
		if (obj === clock && what === \tempo) {
			var newTempo = args[0];
			if (newTempo != tempo  &&  newTempo.isNil.not) {
				tempo = newTempo;
				this.sendTo(\tempo, \set, tempo);
			}
		}
	}

	receiveId { |id, msg, args|
		case
		{ id == \tempo && msg == \set } {
			var newTempo = args[0];
			if (newTempo != tempo) {
				tempo = newTempo;
				clock.etempo = tempo;  // change it right now
			}
		}

		{ id == \clock } {
			// This could slave the TempoClock to incoming MIDI RTC messages.
			// But not now.
		}

		{ id == \trigger} {
			if (playing) { this.pause } { this.play }
		}
	}
}


CWMidiClock : CWControl {
	var midiFuncs, midiOut;

	*new { |point, devId, midiOut |
		^super.new().initMidiClock(point, devId, midiOut )
	}
	initMidiClock { |point, devId, out |
		if (devId.isNil) {
			midiFuncs = [];
		} {
			midiFuncs = [
				MIDIFunc.midiClock  ( { this.send(\midiClock); }, devId ),
				MIDIFunc.start      ( { this.send(\start); }, devId ),
				MIDIFunc.continue   ( { this.send(\continue); }, devId ),
				MIDIFunc.stop       ( { this.send(\stop); }, devId ),
			];
		};
		if (out.isNil.not) {
			midiOut = out;
		};

		this.connect(point);
	}
	free {
		midiFuncs.do (_.free);
		midiFuncs = nil;
		midiOut = nil;
		super.free;
	}

	midiClock   { midiOut !? _.midiClock(); }
	start       { midiOut !? _.start(); }
	continue    { midiOut !? _.continue(); }
	stop        { midiOut !? _.stop(); }

	sane {
		this.stop;
		this.send(\stop);
	}
}
