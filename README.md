# crunch/clockwise

#### I'm Alpha

This is an early release of a library for managing MIDI controllers within
SuperCollider.

I'm running it as part of my live performance set up, so I know it works
reliably... but the API probably could be polished a fair bit, and I'm sure
there is missing useful functionality.

Oh yeah, and there is no SuperCollider help docs for it yet... just this file!


## What is This?

This is a library for connecting various MIDI controllers and devices with
a SuperCollider patch, and with each other. It's aim is to make connecting
them up very easy, in as few lines of code as possible. And to support a style
more like connecting things togther rather than SC's event like MIDI
programming.

Here's a very simple example, connecting some MIDI knobs to a synth:

```
r = { |freq=256.0, speed=6, volume=0.3|
    Ringz.ar(Impulse.ar(speed, 0, volume), freq, 0.5)
}.play;

(
MIDIIn.connectAll;
c = ClockWise(r);
CmdPeriod.add(c);

c.midiDevice(\nk, "nanoKontrol");

c.synthArg(\freq, \midfreq.asSpec);
c.midiCC(\freq, \nk, 0, 14, inOnly:true);

c.synthArg(\speed, ControlSpec(1, 20));
c.midiCC(\speed, \nk, 0, 15, inOnly:true);

c.synthArg(\volume, \amp.asSpec);
c.midiCC(\volume, \nk, 0, 2, inOnly:true);
)
```

Many more complex routings are possible. In my rig I have:

* knobs and faders controlling about two dozen synth params
* knobs and faders that remap to controls on two external synths
* keyboard controller, switchable (via other MIDI buttons) to route
  to the two synths, on four different MIDI channels)
* MIDI clock sent to all devices & the synth, controlled by encoders
* timers displayed on the LED display of a MIDI controller.

## What's Here?

This is a Quark - you can install it with:

```
Quarks.install("https://github.com/mzero/crunch-clockwise.git");
```

or if you downloaded it:

```
Quarks.install("~/Downloads/crunch-clockwise");
```

Once installed, look up the doc for the class `ClockWise`.

## PBJ?!?!

Also in this directory is the giant file `pbj.scd`. This is my live rig
set up! Probably not directly usable, unless you happen to have a Digitakt,
a PreenFM2, a FaderFox UC44, and a Launchpad Pro.... But a deep example of
the kinds of stuff you can do with this.

Feel free to adapted, and send me links to your performances!

