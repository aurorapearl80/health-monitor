package com.monitor.health.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.monitor.health.CallInvitationApi;
import com.monitor.health.services.IncomingCallService;

/**
 * Handles the Decline action button on the incoming-call notification.
 * Accept is deliberately NOT handled here — Android blocks starting an
 * Activity from a plain background BroadcastReceiver, so Accept is wired as
 * an Activity PendingIntent straight to IncomingCallActivity (auto-accept)
 * instead — see IncomingCallService.
 */
public class CallActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        int callInvitationId = intent.getIntExtra(IncomingCallService.EXTRA_CALL_INVITATION_ID, 0);

        if (IncomingCallService.ACTION_DECLINE.equals(action) && callInvitationId > 0) {
            CallInvitationApi.respond(ctx, callInvitationId, "declined");
        }

        // Stop ringing either way
        ctx.stopService(new Intent(ctx, IncomingCallService.class));
    }
}
