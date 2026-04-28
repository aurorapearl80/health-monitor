package com.monitor.health.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.monitor.health.services.IncomingCallService;
import com.monitor.health.ui.IncomingCallActivity;

public class CallActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();

        if (IncomingCallService.ACTION_ANSWER.equals(action)) {
            String caller = intent.getStringExtra(IncomingCallService.EXTRA_CALLER);
            String token  = intent.getStringExtra(IncomingCallService.EXTRA_TOKEN);
            String room   = intent.getStringExtra(IncomingCallService.EXTRA_ROOM);

            Intent i = new Intent(ctx, IncomingCallActivity.class)
                    .putExtra(IncomingCallService.EXTRA_CALLER, caller)
                    .putExtra(IncomingCallService.EXTRA_TOKEN, token)
                    .putExtra(IncomingCallService.EXTRA_ROOM, room)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ctx.startActivity(i);
        }

        // Stop ringing either way
        ctx.stopService(new Intent(ctx, IncomingCallService.class));
    }
}