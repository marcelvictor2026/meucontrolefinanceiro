package br.com.meucontrolefinanceiro;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
 SQLiteDatabase db; LinearLayout layout; String area="Pessoal",periodo="Mensal";
 int bg=Color.rgb(14,22,33), panel=Color.rgb(28,39,54), green=Color.rgb(52,211,153), red=Color.rgb(251,113,133);
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(12,12,12,12);return t;}
 Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);
 db=openOrCreateDatabase("financas.db",MODE_PRIVATE,null);
 db.execSQL("CREATE TABLE IF NOT EXISTS lancamentos(id INTEGER PRIMARY KEY,area TEXT,tipo TEXT,descricao TEXT,valor INTEGER,data INTEGER)");
 render();}
 String reais(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(cents/100.0);}
 void render(){
 ScrollView sc=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(14,14,14,14);layout.setBackgroundColor(bg);sc.addView(layout);setContentView(sc);
 layout.addView(text("MEU CONTROLE FINANCEIRO",15,green));
 layout.addView(text("Visão geral — "+area,25,Color.WHITE));
 LinearLayout scopes=new LinearLayout(this);scopes.addView(button("Pessoal",()->{area="Pessoal";render();}),new LinearLayout.LayoutParams(0,-2,1));scopes.addView(button("Empresa",()->{area="Empresa";render();}),new LinearLayout.LayoutParams(0,-2,1));layout.addView(scopes);
 LinearLayout tabs=new LinearLayout(this);for(String p:new String[]{"Semanal","Mensal","Anual"})tabs.addView(button(p,()->{periodo=p;render();}),new LinearLayout.LayoutParams(0,-2,1));layout.addView(tabs);
 long since=System.currentTimeMillis();Calendar cal=Calendar.getInstance();
 if(periodo.equals("Semanal"))cal.add(Calendar.DAY_OF_YEAR,-7);
 else if(periodo.equals("Mensal"))cal.add(Calendar.MONTH,-1);
 else cal.add(Calendar.YEAR,-1);
 since=cal.getTimeInMillis();
 long in=0,out=0;ArrayList<String> rows=new ArrayList<>();
 try(Cursor c=db.rawQuery("SELECT tipo,descricao,valor,data FROM lancamentos WHERE area=? ORDER BY data DESC",new String[]{area})){
 while(c.moveToNext()){
 String tipo=c.getString(0),descricao=c.getString(1);long v=c.getLong(2),data=c.getLong(3);
 if(data>=since){if(tipo.equals("Receita"))in+=v;else out+=v;}
 rows.add((tipo.equals("Receita")?"+ ":"- ")+reais(v)+"  "+descricao+"  "+new SimpleDateFormat("dd/MM",new Locale("pt","BR")).format(new Date(data)));
 }}
 layout.addView(text("Saldo do período: "+reais(in-out),23,green));
 layout.addView(text("Receitas: "+reais(in),19,green));layout.addView(text("Despesas: "+reais(out),19,red));
 layout.addView(button("+ Novo lançamento",this::newEntry));
 layout.addView(text("Últimos lançamentos",21,Color.WHITE));
 if(rows.isEmpty())layout.addView(text("Nenhum lançamento cadastrado.",15,Color.LTGRAY));
 for(int j=0;j<Math.min(40,rows.size());j++)layout.addView(text(rows.get(j),15,Color.WHITE));
 layout.addView(text("Versão inicial de testes: dados locais, sem backup na nuvem.",12,Color.LTGRAY));
 }
 void newEntry(){
 LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(20,8,20,8);
 Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Despesa","Receita"}));
 EditText description=new EditText(this);description.setHint("Descrição (ex.: almoço)");
 EditText amount=new EditText(this);amount.setHint("Valor (ex.: 35,50)");amount.setInputType(8194);
 form.addView(type);form.addView(description);form.addView(amount);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Novo lançamento — "+area).setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
 dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
 try{
 String raw=amount.getText().toString().trim().replace(".","").replace(",",".");
 long cents=new java.math.BigDecimal(raw).movePointRight(2).longValueExact();
 if(cents<=0)throw new IllegalArgumentException();
 android.content.ContentValues cv=new android.content.ContentValues();
 cv.put("area",area);cv.put("tipo",type.getSelectedItem().toString());cv.put("descricao",description.getText().toString());cv.put("valor",cents);cv.put("data",System.currentTimeMillis());
 db.insertOrThrow("lancamentos",null,cv);dialog.dismiss();render();
 }catch(Exception e){amount.setError("Informe um valor válido, ex.: 35,50");}
 }));dialog.show();
 }
}