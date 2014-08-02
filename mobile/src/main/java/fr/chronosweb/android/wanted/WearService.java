package fr.chronosweb.android.wanted;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import fr.chronosweb.android.wanted.common.Constants;

/**
 * Created by Jean Kruger on 11/07/14.
 * http://www.chronos-web.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

public class WearService extends WearableListenerService {
    private final static String TAG = "WearService";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(Constants.OPEN_ACTIVITY_PATH)){
            MainActivity.open(this);
        }else if (messageEvent.getPath().equals(Constants.CLOSE_ACTIVITY_PATH)){
            MainActivity.close();
        }else if (MainActivity.isOpened()){
            if (!MainActivity.isOnForeground()){
                MainActivity.open(this);
            }

            MainActivity.getSingleton().turnScreenOn();

            if (messageEvent.getPath().equals(Constants.SWITCH_RING_PATH)){
                MainActivity.getSingleton().switchRing();
            }else if (messageEvent.getPath().equals(Constants.SWITCH_VIBRATE_PATH)){
                MainActivity.getSingleton().switchVibrate();
            }else if (messageEvent.getPath().equals(Constants.SWITCH_FLASH_PATH)){
                MainActivity.getSingleton().switchFlash();
            }
        }
    }
}
