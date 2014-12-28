package stan.octo.pod;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.http.POST;
import retrofit.http.Path;

public class Main extends Activity implements View.OnClickListener
{
    private TextView tv_message;//элемент активити к которому мы будем часто обращаться (вывод сообщений)
    //
    private static String CHBKEY;//ключ для хэшмапа (ресурс рисунка чекбокса)
    private static String TXTKEY;//ключ для хэшмапа (текст)
    private static ArrayList<HashMap<String, Object>> cat_list;//лист хешмапов для моего адаптера
    private static MyAdapter ada;//мой адаптер (с собственным обработчиком перерисовки)
    //
    public static String url;//главный урл
    public static String json_url;//урл объекта к которому будем обращаться по пути главного урла
    private List<Category> actual_cat;//актуальный лист категорий (будет использоваться, когда данные уже получены)
    public static boolean process;//флаг означающий, что идёт процесс получения и подготовки данных (задействованы переменные, которые используются при перерисовки, но к ним нельзя обращаться)
    //
    public class Category//мой класс специально созданный для хранения информации полученой в задании
    {
        public int id;//числовой идентификатор категории
        public String name;//название категории
        public boolean isFolder;//флаг означающий папка ли это
        public String avatarLink;//урл к картинке для категории
        public Bitmap avatar;//битмап в котором будет храниться картинка данной категории
        public List<Category> children;//дети внутри категории (если есть)
        public boolean checked;//флаг означающий, что в данный момент элемент не активирован
    }
    public class OctoData//мой класс специально созданный чтобы вытянуть шапку объекта
    {
        public Object[] meta;//метаданные (по заданию пусто)
        public List<Category> data;//нужные данные
        public int status;//значение статуса объекта (200 - хорошо...)
    }
    public interface IOctoDataAPI//интерфейс созданный для создания пост запроса
    {
        @POST("/{url}")
        public OctoData getCategory(@Path("url") String data_url);
    }
    //
    public class MyAdapter//мой адаптер для листвью
            extends SimpleAdapter
    {
        private final Context context;
        private final ArrayList<HashMap<String, Object>> values;
        public MyAdapter(Context context,
                         ArrayList<HashMap<String, Object>> list,
                         int id,
                         String[] f,
                         int[] t)
        {
            super(context,list,id,f,t);
            this.context = context;
            this.values = list;
        }
        @Override
        public View getView(int p, View convertView, ViewGroup parent)//переопределение было необходимо для меня, чтобы реализовать динамическую подстановку картинок в элемент листвью
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.item, parent, false);//получаем вью который видим сейчас (с данными по умолчанию)
            if(process)//проверяем не идёт ли процесс сбора данных
            {
                if(convertView!=null)//если последнее известное значение вью не нул
                {
                    return convertView;//то показываем его
                }
                else
                {
                    return rowView;//если нулл то показываем вью по умолчанию
                }
            }
            HashMap<String, Object> hm = values.get(p);//получаем хэшмэп для конкретного элемента листвью
            ImageView cb = (ImageView) rowView.findViewById(R.id.chb);
            cb.setImageResource((Integer) hm.get(CHBKEY));//присваиваем картинку для чекбокса
            TextView tv = (TextView) rowView.findViewById(R.id.text);
            tv.setText(hm.get(TXTKEY).toString());//присваиваем текст
            ImageView icon = (ImageView) rowView.findViewById(R.id.icon);
            if(actual_cat.size()>=p && actual_cat.get(p)!=null)//если мы можем достать картинку по номеру элемента из нашего актуального листа каталогов
            {
                icon.setImageBitmap(actual_cat.get(p).avatar);
            }
            else
            {
                icon.setImageResource(R.drawable.error);//применим значек ошибки в случае если не можем :(
            }
            if(actual_cat.get(p).checked)//если элемент не выбран, то делаем три элемента полупрозрачными
            {
                cb.setAlpha((float)0.5);
                icon.setAlpha((float)0.5);
                tv.setAlpha((float)0.5);
            }
            return rowView;
        }
    }
    //
    public class WatchData//мой класс созданный для работы в асинктаске
    {
        IOctoDataAPI octo_api;//наша апишка для выполнения запросов
        Activity myAc;//ссылка на активити
        public WatchData(IOctoDataAPI oa, Activity a)
        {
            octo_api = oa;
            myAc = a;
        }
    }
    private WatchData wData;//отсюда мы будем выполнять запрос
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //прописываем настройки элементов активити
        Button btnok = (Button) findViewById(R.id.b_ok);
        btnok.setOnClickListener(this);
        Button btncncl = (Button) findViewById(R.id.b_cancel);
        btncncl.setOnClickListener(this);
        ListView lv = (ListView)findViewById(R.id.lvMain);
        tv_message = (TextView)findViewById(R.id.textMessage);
        tv_message.setText("in process!");
        tv_message.setVisibility(View.GONE);
        //теперь настраиваем всё для связи
        CHBKEY = "check_box";
        TXTKEY = "text";
        url =
                //"http://pinplan.anton.octopod.com/catalogue";
                "https://raw.githubusercontent.com/kepocnhh/OctopodTest/master";
        json_url =
                "my_task_just";
        process = false;
        actual_cat = new ArrayList<Category>();
        cat_list = new ArrayList<HashMap<String, Object>>();
        ada =
            new MyAdapter(
                this,
                cat_list,
                R.layout.item,
                new String[]
                {
                    CHBKEY,
                    TXTKEY
                },
                new int[]
                {
                    R.id.chb,
                    R.id.text
                });
        lv.setAdapter(ada);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int p, long id)//обрабатываем нажатие на элемент листвью
            {
                actual_cat.get(p).checked = !actual_cat.get(p).checked;//меняем значение нажатия на противоположное
                int ch;
                if(!actual_cat.get(p).checked)//и в зависимости от этого назаначаем картинку для чекбокса
                {
                    ch = R.drawable.checkmark_on;
                }
                else
                {
                    ch = R.drawable.checkmark_off;
                }
                cat_list.set(p,add_hm(ch,actual_cat.get(p).name));//изменяем значение в листе хэшмапа
                ada.notifyDataSetChanged();//обновляем листвью
            }
        });
        //создаём рестадаптер и апи для выполнения запроса
        RestAdapter.Builder rb = new RestAdapter.Builder();
        rb.setEndpoint(url);
        rb.setLogLevel(RestAdapter.LogLevel.FULL);
        rb.setClient(new OkClient(new OkHttpClient()));
        RestAdapter restAdapter = rb.build();
        IOctoDataAPI octo_api = restAdapter.create(IOctoDataAPI.class);
        wData = new WatchData(octo_api, this);
        //
        engage();
    }
    static HashMap<String, Object> add_hm(int cb,String txt)//метод для создания хэшмапа
    {
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put(CHBKEY,cb);
        hm.put(TXTKEY,txt);
        return hm;
    }
    private void engage()//метод, который начинает обновлять данные
    {
        process = true;//процесс начался
        tv_message.setVisibility(View.VISIBLE);//показываем текствью с выводом
        new OctoThread().execute(wData);//запускаем асинктаск
    }
    public class OctoThread extends AsyncTask<WatchData, Integer, Integer>//класс сделаный специально для работы с сетью (необходим неглавный поток)
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            tv_message.setText(tv_message.getText().toString()+"\ngetting data...");
        }

        @Override
        protected Integer doInBackground(WatchData... param)
        {
            OctoData all_data = null;
            final Activity ac = param[0].myAc;
            IOctoDataAPI octo_api = param[0].octo_api;
            try
            {
                all_data = octo_api.getCategory(json_url);//получаем всю необходимую информацию
            }
            catch (Exception e)
            {
                showtoastsoc(e.getMessage(), ac);//если произошла ошибка то просто выходим
                return -1;
            }
            //
            if(!check_octo_data(all_data))
            {
                showtoastsoc("Error :(", ac);//если при проверке произошёл сбой выведем сообщение и выходим
                return -2;
            }
            else
            {
                actual_cat = all_data.data.get(0).children;//применяем новый лист категорий
                publishProgress(0);//получили данные
                create_category_list(ac.getApplicationContext());//создаём всё необходимое
                publishProgress(1);//лист хешмапов создан
            }
            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            if(progress[0]==0)
            {
                tv_message.setText(tv_message.getText().toString()+" [OK]\ncreate list...");
            }
            else if(progress[0]==1)
            {
                tv_message.setText(tv_message.getText().toString()+" [OK]\nending...");
            }
        }

        @Override
        protected void onPostExecute(Integer result)//когда закончили
        {
            process = false;//останавливаем процесс
            tv_message.setVisibility(View.GONE);//убираем текствью
            tv_message.setText("in process!");
            ada.notifyDataSetChanged();//перерисовываем листвью
        }
    }
    private boolean check_octo_data(OctoData d)//проверяем данные
    {
        if(d.status!=200)//если статус не 200 это плохо
        {
            return false;
        }
        if(d.data.size()!=1)//в задании был только один элемент в массиве data (не уверен хорошо или плохо проверять так конкретно)
        {
            return false;
        }
        if(d.data.get(0).children == null)//проверяем если ли необходимые нам дети
        {
            return false;
        }
        return true;
    }
    private void create_category_list(Context c)
    {
        cat_list.clear();//очищаем старый лист хэшмапов
        for(int i=0;i<actual_cat.size();i++)//прогоняем по нашему новому листу категорий
        {
            try
            {
                actual_cat.get(i).avatar = Picasso.with(c)//пытаемся достать битмап по урлу (я использовал для этого библиотеку Picasso, такой метод кроме того как достаёт битмап ещё и сам занимается кэшированием и обновлением данных)
                        .load(actual_cat.get(i).avatarLink)
                        .placeholder(R.drawable.error)
                        .error(R.drawable.error).get();
            }
            catch (Exception e)
            {
                actual_cat.get(i).avatar = BitmapFactory.decodeResource(getResources(), R.drawable.error);//если попытка не удалась присваиваем картинку ошибки
            }
            cat_list.add(add_hm(R.drawable.checkmark_on, actual_cat.get(i).name));//добавляем новый элемент в лист хешмапов
        }
    }
    private void buton_Ok()//нажатие на кнопку обновления
    {
        if(process)//если в процессе обновления то ничего не делать
        {
            return;
        }
        engage();
    }
    private void buton_Cancel()//нажатие на кнопку выход
    {
        System.exit(0);
    }
    void showtoastsoc(final String s,final Activity MActivity)//функция сделанная специально для конкретно этого случая работы в асинктаске (вывоб тостов с сообщениями)
    {
        MActivity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast toast = Toast.makeText(MActivity,s, Toast.LENGTH_SHORT);//задает выравнивание, остальные два параметры задают, на сколько пикселей будет смещено сообщение
                toast.setGravity(Gravity.CENTER, 0, 0); // координаты сообщения
                toast.show();
            }
        });
    }
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.b_ok:
                buton_Ok();
                break;
            case R.id.b_cancel:
                buton_Cancel();
        }
    }
    //
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings://нажатие на settings
                if(!process)//если в процессе то ничего не делаем
                new URL_dlg(this);//создаём и открываем диалог для редактирования дополнительного урла
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
