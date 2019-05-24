/***
 *** crunch/ presets:
 ***
 ***       Peanut Butter Jam
 ***        - a beat sync'd effects sandwich for live performance
 ***/

/* === TO DO ===

[] Looper
[] MIDI looper

[] delay volume control, perhaps, send, not return?
[] Beat division display for stutter?
[] Digitakt delay setting, to be able to get triplets

[] Link

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

	~gateRates = [0, 8, 6, 4, 3, 2, 1, 0.5];
		// divisions of a beat, 0 means none

	Routine({
		~beatSyncBus = Bus.control(s, 1);

		~pbj = {
			arg tempo = 2, beatsPerBar = 4,

			gateSelect = 0,

			hpSweep = 20, lpSweep = 20000,

			dlyLevel = 0.0, dlyMix = -0.75, dlyBeats = 8, dlyDecay = 1.4, dlyPos = -0.3,
			revLevel = 0.2, revRoom = 0.6, revDamp = 0.7,
			dryLevel = 0.8,

			loGainDb = 0.0, midGainDb = 0.0, hiGainDb = 0.0, loFreq = 80, hiFreq = 2000,

			outLevel = 0.7
			;

			var beat = Phasor.ar(InTrig.kr(~beatSyncBus.index), tempo / s.sampleRate, 0, beatsPerBar);
			// a useful ar signal that is the beat within the bar

			var in = SoundIn.ar([0,1]);

			var sweepHi = BHiPass.ar(in,       Lag2UD.kr(hpSweep, 0.05, 0.01));
			var sweepLo = BLowPass.ar(sweepHi, Lag2UD.kr(lpSweep, 0.05, 0.01));
			var sweep = sweepLo;

			// these feel better than the more severe BLowPass4 / BHiPass4
			// or the less sever LPF / HPF

			var gateRate = Select.kr(gateSelect, ~gateRates);
			var gateOn = Lag2UD.kr(gateSelect > 0, 0.05, 0.05);
			var gate = Lag2UD.ar((beat * gateRate).frac < 0.5, 0.005, 0.010);
			var gated = sweep * (gate * gateOn + (1 - gateOn));

			var sig = gated;

			var slowestTempo = 50/60;
			var slowest16th = slowestTempo.reciprocal/4;
			var maxDelay = 16*slowest16th;

			var time16th = Lag2.kr(tempo.reciprocal/4, 0.75);
			var dlyTime =  Lag2.kr(dlyBeats*time16th,  0.75);

			var dlyFixL = CombL.ar(sig[0], 3*slowest16th, 3*time16th, dlyDecay);
			var dlyFixR = CombL.ar(sig[1], 5*slowest16th, 5*time16th, dlyDecay);
			var dlyVar =  CombL.ar(sig,   16*slowest16th, dlyTime,    dlyDecay);
			var dly = LeakDC.ar(XFade2.ar([dlyFixL, dlyFixR], dlyVar, dlyMix)) * dlyLevel;
			var splitDly = Pan2.ar(dly, dlyPos);
			var dlyToMix = [splitDly[0][0], splitDly[1][0]];
			var dlyToRev = [splitDly[0][1], splitDly[1][1]];

			var rev = FreeVerb2.ar(
				sig[0] + dlyToRev[0],
				sig[1] + dlyToRev[1],
				mix:1, room:revRoom, damp:revDamp,
				mul: revLevel);

			var dry = sig * dryLevel;

			var sum = Mix.new([dry, rev, dlyToMix]);

			var eqL = sum * midGainDb.dbamp;
			var eqM = BLowShelf.ar(eqL, loFreq, db:(loGainDb - midGainDb));
			var eqH = BHiShelf.ar(eqM, hiFreq, db:(hiGainDb - midGainDb));

			var eqAll = eqH;
			var out = eqAll * outLevel;

			Out.ar(0,  out);
			SendPeakRMS.ar(max(out[0], out[1]), 2, 1, "/outMeter");
		}.play;

		~server.sync;

		~cw = {
			var cw = ClockWise(~pbj);
			CmdPeriod.add(cw);

			cw.midiDevice(\ff, "FaderFox");
			cw.midiDevice(\dt, "Digitakt");

			cw.action(\resync, { cw.syncAll(); });
			cw.midiTriggerButton(\resync, \ff, 12, 15);


			// 3 Band Eq

			cw.synthArg(\loGainDb, \boostcut.asSpec);
			cw.synthArg(\midGainDb, \boostcut.asSpec);
			cw.synthArg(\hiGainDb, \boostcut.asSpec);
			cw.midiCC(\loGainDb, \ff, 12, 82);
			cw.midiCC(\midGainDb, \ff, 12, 83);
			cw.midiCC(\hiGainDb, \ff, 12, 84);

			cw.synthArg(\loFreq, \midfreq.asSpec);
			cw.synthArg(\hiFreq, ControlSpec(440, 10000, \exp));
			cw.midiCC(\loFreq, \ff, 12, 86);
			cw.midiCC(\hiFreq, \ff, 12, 87);

			cw.triggerSane(\eqSane, [\loGainDb, \midGainDb, \hiGainDb, \loFreq, \hiFreq]);
			cw.midiTriggerButton(\eqSane, \ff, 12, 10);

			// Stutter Gate

			cw.synthArg(\gateSelect, ControlSpec(0, ~gateRates.size));
			cw.midiCC(\gateSelect, \ff, 12, 112);

			// Reverb

			cw.synthArg(\revRoom);
			cw.synthArg(\revDamp);
			cw.midiCC(\revRoom, \ff, 12, 100);
			cw.midiCC(\revDamp, \ff, 12, 101);

			cw.triggerSane(\revSane, [\revRoom, \revDamp]);
			cw.midiTriggerButton(\revSane, \ff, 12, 12);


			// Delay

			cw.synthArg(\dlyDecay, ControlSpec(0, 15));
			cw.midiCC(\dlyDecay, \ff, 12, 108);

			cw.synthArg(\dlyMix, \bipolar.asSpec);
			cw.midiCC(\dlyMix, \ff, 12, 109);

			cw.synthArg(\dlyBeats, ControlSpec(1, 16, 'lin', 1));
			cw.midiCC(\dlyBeats, \ff, 12, 110);

			cw.synthArg(\dlyPos, \bipolar.asSpec);
			cw.midiCC(\dlyPos, \ff, 12, 111);

			cw.triggerSane(\dlySane, [\dlyDecay, \dlyMix, \dlyBeats]);
			cw.midiTriggerButton(\dlySane, \ff, 12, 13);


			// Effects

			cw.synthArg(\hpSweep, \freq.asSpec);
			cw.synthArg(\lpSweep, \freq.asSpec);
			cw.midiCC(\hpSweep, \ff, 12, 114);
			cw.midiCC(\lpSweep, \ff, 12, 115);

			cw.triggerSane(\efxSane, [\hpSweep, \lpSweep, \gateSelect]);
			cw.midiTriggerButton(\efxSane, \ff, 12, 14);


			// Levels

			cw.synthArg(\dryLevel);  // \amp.asSpec might be more common here
			cw.synthArg(\revLevel);  // but it doesn't give as fine a control
			cw.synthArg(\dlyLevel);  // over the extreme end, which in this case
			cw.synthArg(\outLevel);  // is more useful
			cw.midiCC(\revLevel,  \ff, 12, 12);
			cw.midiCC(\dlyLevel,  \ff, 12, 13);
			cw.midiCC(\dryLevel,  \ff, 12, 14, ControlSpec(127, 0));
			cw.midiCC(\outLevel,  \ff, 12, 15);


			// Digitakt Track Volumes

			(0..7).do { |i|
				var sym = "dtMix%".format(i).asSymbol;
				cw.midiCC(sym, \ff, i, 95);
				cw.midiCC(sym, \dt, i, 95);
				cw.saneValue(sym, 100/127);
			};

			// MIDI Performance Router

			cw.midiDevice(\nk, "nanoKey");
			cw.midiDevice(\lp, "Launchpad",
				Platform.case(
					\linux,       { "MIDI 2" },
					/* default */ { "Standalone" }
			));

			cw.midiPerf(\perfIn, \nk);
			cw.midiPerf(\perfIn, \lp, 0);

			cw.midiDevice(\mm, "MicroMonsta");
			cw.midiDevice(\ps, "PiSound");

			(0..11).do { |i|
			  var sym = "dtTrack%".format(i).asSymbol;
			  cw.midiPerf(sym, \dt, i, outOnly:true);
			};

			cw.midiPerf(\mmDirect, \mm, 8, outOnly:true);

			// Launchpad performance selector

			(0..7).do { |i|
				cw.midiRadioButton(\perfSelect, i, \lp, 0, cc:(i+1));
			};
			(8..11).do { |i|
				cw.midiRadioButton(\perfSelect, i, \lp, 0, cc:(19+(10*(i-8))));
			};
			cw.midiRadioButton(\perfSelect, 12, \lp, 0, cc:10);

			cw.saneValue(\perfSelect, 12);
			cw.select(\perfIn, \perfSelect,
				(0..11).collect({ |i| "dtTrack%".format(i).asSymbol }) ++ [\mmDirect]);
			cw.midiPerf(\mmDirect, \dt, 8, inOnly:true);

			// BopPad Routing

			cw.midiDevice(\bp, "BopPad");

			// the 1-4 & 5-8 presets go directly to DT at all times
			cw.midiPerf(\perfDrum, \bp, 12, inOnly:true);
			cw.midiPerf(\perfDrum, \dt, 0, outOnly:true);

			// the pitched preset goes to the performance router
			cw.midiPerf(\perfIn, \bp, 13, inOnly:true);

			cw.midiProgram(\drumProg, \bp, 0, outOnly:true);
			(1..3).do { |i|
				cw.midiRadioButton(\drumProg, i, \lp, 0, cc:(89-(10*(i-1))));
			};
			cw.saneValue(\drumProg, 3);

			// Digitakt Mutes
			(0..7).do { |i|
				var sym = "dtMute%".format(i).asSymbol;
				cw.midiCC(sym, \dt, i, 94, unmapped:true);
				cw.midiRadioButton(sym, 1, \ff, 12, note:i, toggle:0);
				cw.saneValue(sym, 0);
			};

			// Output meter
			cw.midiRadioButton(\outMeter, -35.0, \ff, 12, note:8, outOnly:true, orGreater:true);
			cw.midiRadioButton(\outMeter, -15.0, \ff, 12, note:9, outOnly:true, orGreater:true);
			cw.midiRadioButton(\outMeter,  -9.0, \ff, 12, note:10, outOnly:true, orGreater:true);
			cw.midiRadioButton(\outMeter,  -3.0, \ff, 12, note:11, outOnly:true, orGreater:true);

			// Synth Volume Levels

			(8..11).do { |i|                 // channels 9 ~ 12, numbered from 0
				var sym = "synVol%".format(i).asSymbol;
				cw.midiCC(sym, \ff, 12, i);  // faders 9 thru 12
				cw.midiCC(sym, \mm, i, 7);   // standard volume CC
				cw.saneValue(sym, 100/127);  // nominal volume level
			};

			// Tempo & Clock

			cw.tempoClock(\tempo, \clock, ~metro);
			cw.midiClock(\clock, \dt, outOnly:true);
			cw.midiClock(\clock, \mm, outOnly:true);
			cw.midiClock(\clock, \ps, outOnly:true);

			cw.synthArg(\tempo);
			cw.warp(\tempo, \bpm, mul:60);
			cw.midiEncoder(\bpm, \ff, 12, 120, 1, inOnly:true);
			cw.midiEncoder(\bpm, \ff, 12, 121, 0.1, inOnly:true);
			cw.warp(\bpm, \bpm10, mul:10);
			cw.midiBend(\bpm10, \ff, 12, unmapped:true, outOnly:true);

			cw.midiCC(\beat, \ff, 12, 122, unmapped:true, outOnly:true);
			cw.midiTriggerButton(\sendStart, \ff, 12, note:122);
			cw.action(\sendStart, {
				metro.playNextBar({
					cw.point(\clock).distribute(\start);
					nil;
				});
				"MIDI start queued up".postln;
			});

			// Timers

			{
				var t1running = false;

				var t2 = 0;
				var t2running = true;

				var setUpTime = { |sym, minCC, secCC|
					var minSym = (sym.asString ++ "min").asSymbol;
					var secSym = (sym.asString ++ "sec").asSymbol;

					cw.action(sym, { |t|
						if (t >= 0) {
							cw.set(minSym, (t / 60).floor);
							cw.set(secSym, t % 60);
						} {
							cw.set(minSym, 100);
							cw.set(secSym, 100);
						}
					});

					cw.midiCC(minSym, \ff, 12, minCC, unmapped:true, outOnly:true);
					cw.midiCC(secSym, \ff, 12, secCC, unmapped:true, outOnly:true);
				};

				var update = { |blink=false|
					cw.set(\t1,
						if(s.recorder.isRecording && (t1running || blink),
						   s.recorder.duration,
						   -1));
					cw.set(\t2,
						if(t2running || blink,
						t2,
						-1));
				};

				setUpTime.value(\t1, 124, 125);
				setUpTime.value(\t2, 126, 127);



				cw.midiTriggerButton(\t1reset, \ff, 12, 124, inOnly:true);
				cw.action(\t1reset, {
					s.stopRecording;
					t1running = false;
					update.value;
				});

				cw.midiTriggerButton(\t1run, \ff, 12, 125, inOnly:true);
				cw.action(\t1run, {
					t1running = t1running.not;
					if (t1running)
						{ s.record; }
						{ s.pauseRecording; };
					update.value;
				});

				cw.midiTriggerButton(\t2reset, \ff, 12, 126, inOnly:true);
				cw.midiProgram(\t2reset, \dt, 13, inOnly:true);
				cw.action(\t2reset, { t2 = 0; update.value; });

				cw.midiTriggerButton(\t2run, \ff, 12, 127, inOnly:true);
				cw.action(\t2run, { t2running = t2running.not; update.value; });

				SystemClock.sched(0, Routine({
					var blink = true;
					3.yield;

					loop {
						if (t2running) { t2 = t2 + 1 };
						blink = blink.not;
						update.value(blink);
						1.yield;
					}
				}));

				update.value;

			}.value;

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

