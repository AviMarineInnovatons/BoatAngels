package in.avimarine.boatangels.general;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Toast;
import in.avimarine.boatangels.R;


public class DialogUtils {

  private static Toast toast;

  public static void show(Context context, String message, int toastLength) {
    if (message == null) {
      return;
    }
    int l;
    if (toastLength == 0) {
      l = Toast.LENGTH_SHORT;
    } else {
      l = Toast.LENGTH_LONG;
    }
    if (toast == null && context != null) {
      toast = Toast.makeText(context, message, l);
    }
    if (toast != null) {
      toast.setText(message);
      toast.show();
    }
  }

  public static Dialog createDialog(Context context, int titleId, int messageId,
      DialogInterface.OnClickListener positiveButtonListener,
      DialogInterface.OnClickListener negativeButtonListener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.version_not_supported_dialog_title);
    builder.setMessage(messageId);
    builder.setPositiveButton(R.string.dlg_ok, positiveButtonListener);
    builder.setNegativeButton(R.string.dlg_cancel, negativeButtonListener);

    return builder.create();
  }

  public static Dialog createDialog(Context context, int titleId, int messageId, View view,
      DialogInterface.OnClickListener positiveClickListener,
      DialogInterface.OnClickListener negativeClickListener) {

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(titleId);
    builder.setMessage(messageId);
    builder.setView(view);
    builder.setPositiveButton(R.string.dlg_ok, positiveClickListener);
    builder.setNegativeButton(R.string.dlg_cancel, negativeClickListener);

    return builder.create();
  }
}