/*

ClockWise is a "control user interface kit" that allows the interconnections
between controls and things to be controlled.

Unlike UGens, patches in CW are inherintly bidirectional: If a physical control,
an on-screen control, and a synth arg are all connected together, then changing
the physical control will update both the screen control and the synth.
Similarly, changing the screen control will change the synth, and send back
to the physical control.

*/

CWPoint {
    // Point in the control graph.

    // Performance data is distributed to and from all controls connected
    // to the point. If peformance data comes from a control, it is not sent
    // echoed back.

    var connections;

    *new { ^super.new().initPoint }
    initPoint { connections = List(); }
    free {
        var connections_ = connections;
        connections = nil;
        connections_.do(_.at(0).free);
        super.free;
    }

    addControl { |ctl, id=nil|
        // Connect a control to this point. The supplied id is simply passed
        // back in messages, so the control may distinguish multiple points
        // it is connected to.
        connections.add([ctl, id])
    }

    distributeFrom { |fromCtl, msg, args=nil|
        connections.do { |conn|
            var ctl = conn[0], id = conn[1];
            if ((ctl === fromCtl).not)
            {
                ctl.receiveId(id, msg, args);
            };
        };
    }

    distribute { | msg, args=nil | this.distributeFrom(nil, msg, args); }

    performFirst { | msg, args=nil |
        // send msg to the first control that responds to a message
        connections.do { |conn|
            var ctl = conn[0];
            if (ctl.respondsTo(msg)) {
                ^ctl.performList(msg, args);
            }
        }
        ^nil
    }

    hasResponder { |symbol|
        connections.do { |conn|
            if (conn[0].respondsTo(symbol)) {
                ^true;
            }
        };
        ^false
    }

    // convience methods
    sane { ^this.performFirst(\sane) }
    sync { ^this.performFirst(\sync) }
}


CWControl {
    // A control that sends and receives performance data.

    // Controls may be connected to more than one point. The connected points
    // are identified by ids that can then be used to distinguish outgoing and
    // incoming data.
    // The common case, of a single connected point, uses an id of nil, and
    // has methods to make that easy.

    var point;      // the point associated with id of nil
    var points;     // a dictionary of other points (key'd by id)

    *new { ^super.new().initControl; }
    initControl {
        point = nil;
        points = Dictionary();
    }
    free {  // break circular references
        point = nil;
        points = nil;
        super.free;
    }
    connect { |point_, id=nil|
        if (id.isNil) {
            point = point_;
        } {
            points.put(id, point_);
        };
        point_.addControl(this, id);
    }

    send   { |    msg ... args| point         !? _.distributeFrom(this, msg, args); }
    sendTo { |id, msg ... args| points.at(id) !? _.distributeFrom(this, msg, args); }

    receive { |msg, args|
        if (this.respondsTo(msg)) {
            this.performList(msg, args);
        }
    }
    receiveId { |id, msg, args|
        // override this if the control needs to base response on which point is involved
        if (id.isNil) { this.receive(msg, args); }
    }
}
