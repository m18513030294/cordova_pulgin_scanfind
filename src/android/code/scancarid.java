package com.CarOCR;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class scancarid extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
          String argsstring = args.getString(0);
          this.coolMethod(argsstring,callbackContext);
          return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
             Intent intent = new Intent(cordova.getActivity(),MySurfaceViewActivity.class);
              cordova.getActivity().startActivity(intent);
            callbackContext.success("成功");
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
