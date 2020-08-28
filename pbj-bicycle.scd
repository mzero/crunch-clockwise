/***
 *** crunch/ presets:
 ***
 ***       Peanut Butter Jam
 ***        - a beat sync'd effects sandwich for live performance
 ***/

/* === TO DO ===

[] improve accuracy of beatSyncBus by having the sync routine trigger in advance n ms, then
   send the c_set command w/n ms lag.
*/

(
MIDIIn.connectAll;
s.latency = 0;
s.recChannels = 2;
s.recSampleFormat = "int24";

SynthDef("beat",
	{ arg out = 0, freq = 85, amp = 0.5;
		var env = EnvGen.ar(Env.perc(0,0.1), doneAction: Done.freeSelf);
		Out.ar(out, Ringz.ar(env, freq, 0.075) * amp);
	}
).add;

if (~pbj.isNil.not) {
	// clean up any old one hangin' around
	var pbj = ~pbj;
	~pbj = nil;
	protect { pbj.freeUp };
};

~pbj = Environment.make { |self|
	var metro;

	self.know = true;

	~server = s;
	~metro = metro = LinkClock(135/60);
	metro.latency = 0;

	Routine({
		~beatSyncBus = Bus.control(s, 1);

		~pbj = {
			arg tempo = 2, beatsPerBar = 4,

			dlyFeed = 0.0, dlyLevel = 0.0, dlyMix = -0.75, dlyBeats = 8, dlyDecay = 1.4,
			loGainDb = 0.0, midGainDb = 0.0, hiGainDb = 0.0, loFreq = 80, hiFreq = 2000
			;

			var beat = Phasor.ar(InTrig.kr(~beatSyncBus.index), tempo / s.sampleRate, 0, beatsPerBar);
			// a useful ar signal that is the beat within the bar

			var sig = SoundIn.ar([0,1]);

			var dlyIn = sig * dlyFeed;

			var slowestTempo = 50/60;
			var slowest16th = slowestTempo.reciprocal/4;
			var maxDelay = 16*slowest16th;

			var time16th = Lag2.kr(tempo.reciprocal/4, 0.75);
			var dlyTime =  Lag2.kr(dlyBeats*time16th,  0.75);

			var dlyFixL = CombL.ar(dlyIn[0], 3*slowest16th, 3*time16th, dlyDecay);
			var dlyFixR = CombL.ar(dlyIn[1], 5*slowest16th, 5*time16th, dlyDecay);
			var dlyVar =  CombL.ar(dlyIn,   16*slowest16th, dlyTime,    dlyDecay);
			var dly = LeakDC.ar(XFade2.ar([dlyFixL, dlyFixR], dlyVar, dlyMix)) * dlyLevel;

			var eqL = dly * midGainDb.dbamp;
			var eqM = BLowShelf.ar(eqL, loFreq, db:(loGainDb - midGainDb));
			var eqH = BHiShelf.ar(eqM, hiFreq, db:(hiGainDb - midGainDb));
			var eqAll = eqH;

			var out = eqAll ;

			Out.ar(0,  out);
		}.play;

		~server.sync;

		~cw = {
			var cw = ClockWise(~pbj);
			CmdPeriod.add(cw);

			cw.midiDevice(\nk, "nanoKONTROL", inOnly:true);

			// 3 Band Eq

			cw.synthArg(\loGainDb, \boostcut.asSpec);
			cw.synthArg(\midGainDb, \boostcut.asSpec);
			cw.synthArg(\hiGainDb, \boostcut.asSpec);
			cw.midiCC(\loGainDb, \nk, 15, 90);
			cw.midiCC(\midGainDb, \nk, 15, 91);
			cw.midiCC(\hiGainDb, \nk, 15, 92);

			cw.synthArg(\loFreq, \midfreq.asSpec);
			cw.synthArg(\hiFreq, ControlSpec(440, 10000, \exp));
			cw.midiCC(\loFreq, \nk, 15, 80);
			cw.midiCC(\hiFreq, \nk, 15, 82);

			cw.triggerSane(\eqSane, [\loGainDb, \midGainDb, \hiGainDb, \loFreq, \hiFreq]);
			cw.midiTriggerButton(\eqSane, \ff, 15, 112);


			// Delay

			cw.synthArg(\dlyDecay, ControlSpec(0, 15));
			cw.midiCC(\dlyDecay, \nk, 15, 65);

			cw.synthArg(\dlyMix, \bipolar.asSpec);
			cw.midiCC(\dlyMix, \nk, 15, 68);

			cw.synthArg(\dlyBeats, ControlSpec(1, 16, 'lin', 1));
			cw.midiCC(\dlyBeats, \nk, 15, 66);

			cw.triggerSane(\dlySane, [\dlyDecay, \dlyMix, \dlyBeats]);
			cw.midiTriggerButton(\dlySane, \nk, 15, 75);


			// Levels

			cw.synthArg(\dlyFeed);
			cw.midiCC(\dlyFeed,  \nk, 15, 67);

			cw.synthArg(\dlyLevel);  // over the extreme end, which in this case
			cw.midiCC(\dlyLevel,  \nk, 15, 69);


			// Looper

			cw.midiDevice(\bi, "Feather");

			// nanoKONTROL control -> Looper

			(2..49).do { |i|
				var sym = "cc%".format(i).asSymbol;
				cw.midiCC(sym, \nk, 15, i);
				cw.midiCC(sym, \bi, 15, i);
			};

			// boppad -> Looper

			cw.midiDevice(\bp, "BopPad");
			cw.midiPerf(\beats, \bp, 0, ccs:[70, 71], inOnly:true);
			cw.midiPerf(\beats, \bi, 0, outOnly:true);

			cw.midiDevice(\bp, "BopPad");
			cw.midiPerf(\control, \bp, 1, inOnly:true);
			cw.midiPerf(\control, \bi, 1, outOnly:true);


			// Tempo & Clock

			cw.tempoClock(\tempo, \clock, ~metro);

			cw.synthArg(\tempo);
			cw.warp(\tempo, \bpm, mul:60);
			cw.warp(\bpmCC, \bpm, mul:200, add:40);
			cw.midiCC(\bpmCC, \nk, 15, 64, inOnly:true);

			cw;
		}.value;

		1.sleep;
		~cw.syncAll;

		{
			var cw = ~cw;
			~meterOsc = OSCFunc({|msg|
				var peakdb = msg[3].ampdb;
				cw.set(\outMeter, peakdb);
			}, "/outMeter");
		}.value;

	}).play;

	~metro.playNextBar(Routine({
		loop {
			var beat = metro.beatInBar.floor;
			if (beat == 0) {
				~beatSyncBus.set(1);   // doesn't matter what it is set to, just > 0
			};
			//Synth("beat");
			~cw.set(\beat, beat + 1);  // humans count from 1

			1.yield;
		}
	}));

	~freeUp = { | self |
		self.outMeter.free;
		self.cw.free;
		self.cw = nil;
		self.efx.free;
		self.efx = nil;
		self.metro.stop;
		self.metro = nil;
	}
};
)

