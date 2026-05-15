package com.example.bai3.widget;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.example.bai3.LGRemoteProApp;
import com.example.bai3.MainActivity;
import com.example.bai3.network.CommandBuilder;
import com.example.bai3.network.LGTVClient;

/**
 * Quick Settings tile for toggling TV power.
 * Shows as an active tile when connected to a TV.
 */
public class PowerTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        LGRemoteProApp app = (LGRemoteProApp) getApplicationContext();
        LGTVClient client = app.getTvClient();

        if (client.isConnected()) {
            // Send power off command
            client.sendCommand(CommandBuilder.buildTurnOff());
            updateTileState();
        } else {
            // Open the app for connection
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        }
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        LGRemoteProApp app = (LGRemoteProApp) getApplicationContext();
        LGTVClient client = app.getTvClient();

        if (client.isConnected()) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }
}
