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
import java.io.*;

public class MainActivity extends Activity {
 SQLiteDatabase db; LinearLayout layout; String area="Pessoal",periodo="Mensal";
 int bg=Color.rgb(14,22,33), panel=Color.rgb(28,39,54), green=Color.rgb(52,211,153), red=Color.rgb(251,113,133);
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(12,12,12,12);return t;}
 Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);
 db=openOrCreateDatabase("financas.db",MODE_PRIVATE,null);
 db.execSQL("CREATE TABLE IF NOT EXISTS lancamentos(id INTEGER PRIMARY KEY,area TEXT,tipo TEXT,descricao TEXT,valor INTEGER,data INTEGER)");
 db.execSQL("CREATE TABLE IF NOT EXISTS agenda(id INTEGER PRIMARY KEY AUTOINCREMENT,titulo TEXT NOT NULL,quando INTEGER NOT NULL)");
 if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},123);
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
 long in=0,out=0;ArrayList<String> rows=new ArrayList<>();ArrayList<Long> ids=new ArrayList<>();
 try(Cursor c=db.rawQuery("SELECT id,tipo,descricao,valor,data FROM lancamentos WHERE area=? AND data>=? ORDER BY data DESC",new String[]{area,String.valueOf(since)})){
 while(c.moveToNext()){
 long id=c.getLong(0);String tipo=c.getString(1),descricao=c.getString(2);long v=c.getLong(3),data=c.getLong(4);
 if(tipo.equals("Receita"))in+=v;else out+=v;ids.add(id);
 rows.add((tipo.equals("Receita")?"+ ":"- ")+reais(v)+"  "+descricao+"  "+new SimpleDateFormat("dd/MM",new Locale("pt","BR")).format(new Date(data)));
 }}
 layout.addView(text("Saldo do período: "+reais(in-out),23,green));
 layout.addView(text("Receitas: "+reais(in),19,green));layout.addView(text("Despesas: "+reais(out),19,red));
 layout.addView(text("Comparativo do período",18,Color.WHITE));
 long total=in+out; if(total>0){LinearLayout bars=new LinearLayout(this);bars.setOrientation(1);bars.setPadding(12,8,12,8);bars.setBackgroundColor(panel);addBar(bars,"Receitas",in,total,green);addBar(bars,"Despesas",out,total,red);layout.addView(bars);}else layout.addView(text("Cadastre receitas ou despesas para visualizar o gráfico.",13,Color.LTGRAY));
 layout.addView(button("+ Novo lançamento",this::newEntry));
 layout.addView(button("Exportar todos os lançamentos (CSV)",this::exportCsv));
 layout.addView(text("AGENDA E LEMBRETES",20,green));
 layout.addView(button("+ Agendar lembrete",this::newReminder));
 showAgenda();
 layout.addView(button("Restaurar lançamentos de backup CSV",this::importCsv));
 layout.addView(text("Últimos lançamentos",21,Color.WHITE));
 if(rows.isEmpty())layout.addView(text("Nenhum lançamento cadastrado.",15,Color.LTGRAY));
 for(int j=0;j<Math.min(40,rows.size());j++){final long id=ids.get(j);TextView item=text(rows.get(j)+"  •  toque para editar",15,Color.WHITE);item.setBackgroundColor(panel);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,5);layout.addView(item,lp);item.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Lançamento").setItems(new String[]{"Editar","Excluir"},(d,which)->{if(which==0)editEntry(id);else new AlertDialog.Builder(this).setTitle("Excluir lançamento?").setMessage("Esta ação não pode ser desfeita.").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(a,w)->{db.delete("lancamentos","id=? AND area=?",new String[]{String.valueOf(id),area});render();}).show();}).show());}
 layout.addView(text("Dados locais. Exporte um CSV para backup e guarde-o em local seguro. Ao restaurar, os registros são acrescentados.",12,Color.LTGRAY));
 }
 void addBar(LinearLayout parent,String name,long value,long total,int color){parent.addView(text(name+": "+reais(value),14,Color.WHITE));LinearLayout track=new LinearLayout(this);track.setBackgroundColor(bg);int width=getResources().getDisplayMetrics().widthPixels-80;int barWidth=(int)Math.max(value>0?4:0,Math.round((double)value/total*width));View fill=new View(this);fill.setBackgroundColor(color);track.addView(fill,new LinearLayout.LayoutParams(barWidth,20));parent.addView(track,new LinearLayout.LayoutParams(-1,20));}
 void showAgenda(){
 SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
 try(Cursor c=db.rawQuery("SELECT id,titulo,quando FROM agenda ORDER BY quando ASC LIMIT 50",null)){
 while(c.moveToNext()){
 final int id=c.getInt(0);String name=c.getString(1);long when=c.getLong(2);
 TextView item=text(fmt.format(new Date(when))+" — "+name+" (toque para excluir)",15,Color.WHITE);
 item.setBackgroundColor(panel);layout.addView(item);
 item.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Excluir lembrete?").setMessage(name).setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w)->{ReminderReceiver.cancel(this,id);db.delete("agenda","id=?",new String[]{String.valueOf(id)});render();}).show());
 }
 }
 }
 void newReminder(){
 EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Descrição do compromisso");
 LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(20,8,20,8);form.addView(title);
 Calendar selected=Calendar.getInstance();selected.add(Calendar.HOUR_OF_DAY,1);selected.set(Calendar.SECOND,0);selected.set(Calendar.MILLISECOND,0);
 SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
 Button date=button("Data: "+fmt.format(selected.getTime()),()->{});
 date.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{selected.set(y,m,d);date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show());
 Button time=button("Horário: "+new SimpleDateFormat("HH:mm",new Locale("pt","BR")).format(selected.getTime()),()->{});
 time.setOnClickListener(v->new TimePickerDialog(this,(view,h,min)->{selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,min);time.setText(String.format(new Locale("pt","BR"),"Horário: %02d:%02d",h,min));date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());
 form.addView(date);form.addView(time);
 new AlertDialog.Builder(this).setTitle("Novo lembrete").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Agendar",(d,w)->{
 String name=title.getText().toString().trim();long when=selected.getTimeInMillis();
 if(name.isEmpty()||when<=System.currentTimeMillis()){Toast.makeText(this,"Informe descrição e data futura",Toast.LENGTH_LONG).show();return;}
 ContentValues values=new ContentValues();values.put("titulo",name);values.put("quando",when);
 long id=db.insert("agenda",null,values);if(id>0){ReminderReceiver.schedule(this,(int)id,when);render();Toast.makeText(this,"Lembrete agendado. Ative as notificações.",Toast.LENGTH_LONG).show();}
 }).show();
 }
 void importCsv(){
 new AlertDialog.Builder(this).setTitle("Restaurar backup").setMessage("Importe um arquivo CSV exportado por este aplicativo. Os lançamentos importados serão acrescentados aos existentes; importe uma única vez para evitar duplicatas.").setNegativeButton("Cancelar",null).setPositiveButton("Selecionar arquivo",(d,w)->{
 Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,1002);
 }).show();
 }
 String[] parseCsvLine(String line){
 ArrayList<String> values=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;
 for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(ch==';'&&!quoted){values.add(value.toString());value.setLength(0);}else value.append(ch);}
 values.add(value.toString());return values.toArray(new String[0]);
 }
 void restoreCsv(android.net.Uri uri){
 int count=0;try(InputStream input=getContentResolver().openInputStream(uri)){
 if(input==null)throw new IOException("Arquivo indisponível");
 BufferedReader reader=new BufferedReader(new InputStreamReader(input,"UTF-8"));
 String header=reader.readLine();if(header==null||!header.replace("\uFEFF","").equals("area;tipo;descricao;valor;data"))throw new IOException("Formato de backup inválido");
 ArrayList<ContentValues> entries=new ArrayList<>();String line;SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",new Locale("pt","BR"));fmt.setLenient(false);
 while((line=reader.readLine())!=null){if(line.trim().isEmpty())continue;if(entries.size()>=100000)throw new IOException("Arquivo muito grande");
 String[] fields=parseCsvLine(line);if(fields.length!=5)throw new IOException("Linha inválida");
 if(!fields[0].equals("Pessoal")&&!fields[0].equals("Empresa"))throw new IOException("Área inválida");
 if(!fields[1].equals("Receita")&&!fields[1].equals("Despesa"))throw new IOException("Tipo inválido");
 long cents=parseCents(fields[3]);long date=fmt.parse(fields[4]).getTime();
 ContentValues cv=new ContentValues();cv.put("area",fields[0]);cv.put("tipo",fields[1]);cv.put("descricao",fields[2]);cv.put("valor",cents);cv.put("data",date);entries.add(cv);
 }
 db.beginTransaction();try{for(ContentValues cv:entries){db.insertOrThrow("lancamentos",null,cv);count++;}db.setTransactionSuccessful();}finally{db.endTransaction();}
 Toast.makeText(this,count+" lançamentos restaurados",Toast.LENGTH_LONG).show();render();
 }catch(Exception e){new AlertDialog.Builder(this).setTitle("Falha ao restaurar").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
 }
 void exportCsv(){
 Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
 intent.addCategory(Intent.CATEGORY_OPENABLE);
 intent.setType("text/csv");
 intent.putExtra(Intent.EXTRA_TITLE,"meu-controle-financeiro.csv");
 startActivityForResult(intent,1001);
 }
 long parseCents(String input){String raw=input.trim().replace(" ","");if(raw.contains(","))raw=raw.replace(".","").replace(",",".");else if(raw.matches("[0-9]+\\.[0-9]{3}(\\.[0-9]{3})*"))raw=raw.replace(".","");long cents=new java.math.BigDecimal(raw).movePointRight(2).longValueExact();if(cents<=0)throw new IllegalArgumentException();return cents;}
 String csvCell(String s){return "\"" + s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ") + "\"";}
 @Override protected void onActivityResult(int request,int result,Intent data){
 super.onActivityResult(request,result,data);
 if(result!=RESULT_OK||data==null||data.getData()==null)return;
 if(request==1002){restoreCsv(data.getData());return;}
 if(request!=1001)return;
 try{
  OutputStream stream=getContentResolver().openOutputStream(data.getData());
  if(stream==null)throw new IOException("Sem acesso ao arquivo");
  try(Writer writer=new OutputStreamWriter(stream,"UTF-8")){
   writer.write("\uFEFFarea;tipo;descricao;valor;data\\n");
   try(Cursor c=db.rawQuery("SELECT area,tipo,descricao,valor,data FROM lancamentos ORDER BY data DESC",null)){
    SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",new Locale("pt","BR"));
    while(c.moveToNext()){
     String amount=java.math.BigDecimal.valueOf(c.getLong(3),2).toPlainString().replace('.',',');
     writer.write(csvCell(c.getString(0))+";"+csvCell(c.getString(1))+";"+csvCell(c.getString(2))+";"+csvCell(amount)+";"+csvCell(fmt.format(new Date(c.getLong(4))))+"\\n");
    }
   }
  }
  Toast.makeText(this,"Arquivo CSV salvo com sucesso",Toast.LENGTH_LONG).show();
 }catch(Exception e){new AlertDialog.Builder(this).setMessage("Não foi possível exportar: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
 void editEntry(long id){
 try(Cursor c=db.rawQuery("SELECT tipo,descricao,valor FROM lancamentos WHERE id=? AND area=?",new String[]{String.valueOf(id),area})){
 if(!c.moveToFirst())return;
 String oldType=c.getString(0),oldDescription=c.getString(1);long oldValue=c.getLong(2);
 LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(20,8,20,8);
 Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Despesa","Receita"}));type.setSelection(oldType.equals("Receita")?1:0);
 EditText description=new EditText(this);description.setSingleLine(true);description.setText(oldDescription);
 EditText amount=new EditText(this);amount.setInputType(8194);amount.setText(java.math.BigDecimal.valueOf(oldValue,2).toPlainString().replace('.',','));
 form.addView(type);form.addView(description);form.addView(amount);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Editar lançamento").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
 dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
 try{
 long cents=parseCents(amount.getText().toString());
 if(description.getText().toString().trim().isEmpty()){description.setError("Informe uma descrição");return;}
 ContentValues cv=new ContentValues();cv.put("tipo",type.getSelectedItem().toString());cv.put("descricao",description.getText().toString().trim());cv.put("valor",cents);
 db.update("lancamentos",cv,"id=? AND area=?",new String[]{String.valueOf(id),area});
 dialog.dismiss();render();
 }catch(Exception e){amount.setError("Informe um valor válido, ex.: 35,50");}
 }));dialog.show();
 }
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
 long cents=parseCents(amount.getText().toString());
 if(description.getText().toString().trim().isEmpty()){description.setError("Informe uma descrição");return;}
 android.content.ContentValues cv=new android.content.ContentValues();
 cv.put("area",area);cv.put("tipo",type.getSelectedItem().toString());cv.put("descricao",description.getText().toString());cv.put("valor",cents);cv.put("data",System.currentTimeMillis());
 db.insertOrThrow("lancamentos",null,cv);dialog.dismiss();render();
 }catch(Exception e){amount.setError("Informe um valor válido, ex.: 35,50");}
 }));dialog.show();
 }
}