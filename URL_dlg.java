package stan.octo.pod;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class URL_dlg
        extends Activity
        implements OnClickListener
{
    private EditText et;//поле для ввода урла
    private Dialog dialog;//диалог который мы запускаем
    private Activity ac;//активити из которого мы запускаем
    public URL_dlg(Activity activity)
    {
        this.ac = activity;//сохраняем у себя активити, чтобы потом его можно было использовать при выводе сообщений
        dialog = new Dialog(ac);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);//назначаем диалогу появляться без заголовка (меньше места занимает)
        dialog.setContentView(R.layout.url_dlg);//назначаем диалогу лейаут
        et = (EditText)dialog.findViewById(R.id.ud_et);
        et.setText(Main.json_url);//назначаем полю урл json который сейчас актуален
        ((Button)dialog.findViewById(R.id.ud_b)).setOnClickListener(this);//назначаем лисенер на слик на кнопку (которая одна)
        dialog.show();//показываем диалог
    }

    @Override
    public void onClick(View view)
    {
        if(et.getText().toString().length()==0)//проверяем не пустое ли поле для ввода урла
        {
            Toast.makeText(ac, "Empty!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Main.json_url = et.getText().toString();
            Toast.makeText(ac, "FULL Url now:\n" + Main.url + "/" +Main.json_url, Toast.LENGTH_SHORT).show();
            dialog.cancel();
        }
    }
}
