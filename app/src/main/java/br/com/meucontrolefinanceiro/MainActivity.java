package br.com.meucontrolefinanceiro;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
 SQLiteDatabase db; LinearLayout layout; String area="Pessoal",periodo="Mensal";
 int bg=Color.rgb(5,6,7), panel=Color.rgb(22,22,20), green=Color.rgb(44,211,127), red=Color.rgb(255,93,83), cyan=Color.rgb(214,173,91), gold=Color.rgb(231,190,101);
 int muted=Color.rgb(181,174,158), stroke=Color.rgb(85,68,40);
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
 GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColors(new int[]{color,Color.rgb(10,10,10)});d.setOrientation(GradientDrawable.Orientation.TL_BR);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
 void spacing(int h){View v=new View(this);layout.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
 TextView metric(String label,String amount,int accent){
  TextView t=text(label+"\n"+amount,19,accent);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLineSpacing(dp(5),1);
  t.setPadding(dp(18),dp(17),dp(18),dp(17));t.setBackground(surface(panel,22));t.setElevation(dp(4));return t;
 }
 TextView areaButton(String name,String subtitle,String symbol,int accent){
  boolean active=area.equals(name);
  TextView t=text(symbol+"  "+name+"\n"+subtitle,15,active?bg:Color.WHITE);
  t.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);
  t.setGravity(Gravity.CENTER_VERTICAL);t.setLineSpacing(dp(7),1f);
  t.setPadding(dp(16),dp(20),dp(10),dp(20));
  GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,active?new int[]{Color.rgb(249,220,150),gold,Color.rgb(154,112,45)}:new int[]{panel,Color.rgb(11,11,11)});
  d.setCornerRadius(dp(22));d.setStroke(dp(active?2:1),active?gold:stroke);
  t.setBackground(d);t.setElevation(dp(active?8:2));
  t.setOnClickListener(v->{if(!area.equals(name)){area=name;render();}});
  return t;
 }
 void section(String name){spacing(22);TextView t=text("✦  "+name,18,Color.WHITE);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);layout.addView(t);spacing(5);}
 Button action(String label,Runnable callback,boolean primary){Button b=button(label,callback);b.setTextColor(primary?Color.BLACK:Color.WHITE);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(primary?new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(248,220,151),gold,Color.rgb(163,117,46)}):surface(panel,18));b.setElevation(dp(5));b.setMinHeight(dp(52));return b;}

 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(12,12,12,12);return t;}
 Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setBackground(surface(panel,16));b.setMinHeight(dp(44));b.setOnClickListener(v->action.run());return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
 if(b!=null){area=b.getString("area","Pessoal");periodo=b.getString("periodo","Mensal");}
 db=openOrCreateDatabase("financas.db",MODE_PRIVATE,null);
 db.execSQL("CREATE TABLE IF NOT EXISTS lancamentos(id INTEGER PRIMARY KEY,area TEXT,tipo TEXT,descricao TEXT,valor INTEGER,data INTEGER)");
 try{db.execSQL("ALTER TABLE lancamentos ADD COLUMN origem TEXT DEFAULT ''");}catch(android.database.SQLException ignored){}
 try{db.execSQL("ALTER TABLE lancamentos ADD COLUMN detalhes TEXT DEFAULT ''");}catch(android.database.SQLException ignored){}
 try{db.execSQL("ALTER TABLE lancamentos ADD COLUMN criado_em INTEGER DEFAULT 0");}catch(android.database.SQLException ignored){}
 db.execSQL("CREATE TABLE IF NOT EXISTS agenda(id INTEGER PRIMARY KEY AUTOINCREMENT,titulo TEXT NOT NULL,quando INTEGER NOT NULL)");
 if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},123);
 render();}
 @Override protected void onSaveInstanceState(Bundle state){state.putString("area",area);state.putString("periodo",periodo);super.onSaveInstanceState(state);}
 String reais(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(java.math.BigDecimal.valueOf(cents,2));}
 void render(){
 ScrollView sc=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(dp(18),dp(24),dp(18),dp(36));layout.setBackgroundColor(bg);sc.addView(layout);setContentView(sc);
 LinearLayout brandRow=new LinearLayout(this);brandRow.setGravity(Gravity.CENTER_VERTICAL);
 ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_brand_icon);brandRow.addView(logo,new LinearLayout.LayoutParams(dp(44),dp(44)));
 TextView brand=text("MEU CONTROLE\\nFINANCEIRO",16,Color.WHITE);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brandRow.addView(brand);
 brandRow.setPadding(dp(12),dp(9),dp(12),dp(9));layout.addView(brandRow);spacing(12);
 TextView heading=text("VISÃO GERAL  /  "+area.toUpperCase(new Locale("pt","BR")),12,gold);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);layout.addView(heading);spacing(9);
 TextView switchTitle=text("PAINÉIS",12,muted);switchTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);layout.addView(switchTitle);spacing(5);
 LinearLayout scopes=new LinearLayout(this);scopes.setOrientation(LinearLayout.HORIZONTAL);
 LinearLayout.LayoutParams personalParams=new LinearLayout.LayoutParams(0,dp(102),1);personalParams.setMargins(0,0,dp(6),0);
 LinearLayout.LayoutParams businessParams=new LinearLayout.LayoutParams(0,dp(102),1);businessParams.setMargins(dp(6),0,0,0);
 scopes.addView(areaButton("Pessoal","Minhas finanças","◉",cyan),personalParams);
 scopes.addView(areaButton("Empresa","Gestão do negócio","▣",cyan),businessParams);
 layout.addView(scopes);spacing(10);
 TextView selected=text(area.equals("Pessoal")?"●  FINANÇAS PESSOAIS":"●  FINANÇAS DA EMPRESA",12,gold);
 selected.setTypeface(Typeface.DEFAULT,Typeface.BOLD);layout.addView(selected);spacing(9);
 LinearLayout tabs=new LinearLayout(this);for(String p:new String[]{"Semanal","Mensal","Anual"})tabs.addView(button((periodo.equals(p)?"● ":"")+p,()->{periodo=p;render();}),new LinearLayout.LayoutParams(0,-2,1));layout.addView(tabs);spacing(14);
 long since=System.currentTimeMillis();Calendar cal=Calendar.getInstance();
 if(periodo.equals("Semanal"))cal.add(Calendar.DAY_OF_YEAR,-7);
 else if(periodo.equals("Mensal"))cal.add(Calendar.MONTH,-1);
 else cal.add(Calendar.YEAR,-1);
 since=cal.getTimeInMillis();
 long in=0,out=0;ArrayList<String> rows=new ArrayList<>();ArrayList<Long> ids=new ArrayList<>();
 try(Cursor c=db.rawQuery("SELECT id,tipo,descricao,valor,data,origem,detalhes,criado_em FROM lancamentos WHERE area=? AND data>=? ORDER BY data DESC",new String[]{area,String.valueOf(since)})){
 while(c.moveToNext()){
 long id=c.getLong(0);String tipo=c.getString(1),descricao=c.getString(2);long v=c.getLong(3),data=c.getLong(4);
 if(tipo.equals("Receita"))in+=v;else out+=v;ids.add(id);
 String origem=c.getString(5),detalhes=c.getString(6);long criado=c.getLong(7);
 String dateText=new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",new Locale("pt","BR")).format(new Date(data));
 String row=(tipo.equals("Receita")?"↑ RECEITA  ":"↓ DESPESA  ")+reais(v)+"\n"+descricao;
 if(origem!=null&&!origem.isEmpty())row+="\nOrigem: "+origem;
 if(detalhes!=null&&!detalhes.isEmpty())row+="\nDetalhes: "+detalhes;
 row+="\nEntrada: "+dateText;
 if(criado>0&&criado!=data)row+="\nRegistrado: "+new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",new Locale("pt","BR")).format(new Date(criado));
 rows.add(row);
 }}
 TextView balance=metric("SALDO ATUAL  •  "+periodo.toUpperCase(new Locale("pt","BR")),reais(in-out),in-out>=0?Color.WHITE:red);balance.setTextSize(25);balance.setPadding(dp(22),dp(25),dp(22),dp(25));GradientDrawable balanceBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(35,32,24),Color.rgb(12,12,11),Color.rgb(65,48,23)});balanceBg.setCornerRadius(dp(22));balanceBg.setStroke(dp(1),gold);balance.setBackground(balanceBg);layout.addView(balance);
 spacing(9);LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);
 TextView income=metric("↑ RECEITAS",reais(in),green);TextView expense=metric("↓ DESPESAS",reais(out),red);
 LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.setMargins(0,0,dp(5),0);
 LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.setMargins(dp(5),0,0,0);
 metrics.addView(income,left);metrics.addView(expense,right);layout.addView(metrics);
 section("RESUMO DO PERÍODO");
 long total=in+out; if(total>0){LinearLayout bars=new LinearLayout(this);bars.setOrientation(1);bars.setPadding(dp(14),dp(12),dp(14),dp(12));bars.setBackground(surface(panel,22));addBar(bars,"Receitas",in,total,green);addBar(bars,"Despesas",out,total,red);layout.addView(bars);}else layout.addView(text("Cadastre receitas ou despesas para visualizar o gráfico.",13,Color.LTGRAY));
 spacing(15);layout.addView(action("＋  Novo lançamento",this::newEntry,true));
 
 section("AGENDA");
 layout.addView(action("＋  Agendar lembrete",()->newReminder(-1),false));
 showAgenda();
 section("SEGURANÇA E BACKUP");
 layout.addView(action("↓  Exportar lançamentos (CSV)",this::exportCsv,false));
 layout.addView(action("↑  Restaurar backup (CSV)",this::importCsv,false));
 section("ÚLTIMAS MOVIMENTAÇÕES");
 if(rows.isEmpty())layout.addView(text("Nenhum lançamento cadastrado.",15,Color.LTGRAY));
 for(int j=0;j<Math.min(40,rows.size());j++){final long id=ids.get(j);TextView item=text(rows.get(j)+"  •  toque para editar",15,Color.WHITE);item.setPadding(dp(15),dp(17),dp(15),dp(17));item.setBackground(surface(panel,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(7));layout.addView(item,lp);item.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Lançamento").setItems(new String[]{"Editar","Excluir"},(d,which)->{if(which==0)editEntry(id);else new AlertDialog.Builder(this).setTitle("Excluir lançamento?").setMessage("Esta ação não pode ser desfeita.").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(a,w)->{db.delete("lancamentos","id=? AND area=?",new String[]{String.valueOf(id),area});render();}).show();}).show());}
 layout.addView(text("Dados locais. Exporte um CSV para backup e guarde-o em local seguro. Ao restaurar, os registros são acrescentados.",12,Color.LTGRAY));
 }

 void addBar(LinearLayout parent,String name,long value,long total,int color){parent.addView(text(name+": "+reais(value),14,Color.WHITE));LinearLayout track=new LinearLayout(this);track.setBackground(surface(bg,8));View fill=new View(this);fill.setBackground(surface(color,8));track.addView(fill,new LinearLayout.LayoutParams(0,dp(12),value));track.addView(new View(this),new LinearLayout.LayoutParams(0,dp(12),Math.max(0,total-value)));parent.addView(track,new LinearLayout.LayoutParams(-1,dp(12)));}
 void showAgenda(){
 SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
 try(Cursor c=db.rawQuery("SELECT id,titulo,quando FROM agenda ORDER BY quando ASC LIMIT 50",null)){
 while(c.moveToNext()){
 final int id=c.getInt(0);String name=c.getString(1);long when=c.getLong(2);
 TextView item=text(fmt.format(new Date(when))+" — "+name+" (toque para editar/excluir)",15,Color.WHITE);
 item.setBackground(surface(panel,14));item.setPadding(dp(15),dp(14),dp(15),dp(14));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.setMargins(0,0,0,dp(7));layout.addView(item,ap);
 item.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Editar","Excluir"},(d,which)->{if(which==0)newReminder(id);else new AlertDialog.Builder(this).setTitle("Excluir lembrete?").setMessage(name).setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(a,w)->{ReminderReceiver.cancel(this,id);db.delete("agenda","id=?",new String[]{String.valueOf(id)});render();}).show();}).show());
 }
 }
 }
 void newReminder(int editingId){
 EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Descrição do compromisso");
 LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(20,8,20,8);form.addView(title);
 Calendar selected=Calendar.getInstance();selected.add(Calendar.HOUR_OF_DAY,1);selected.set(Calendar.SECOND,0);selected.set(Calendar.MILLISECOND,0);
 if(editingId>=0){try(Cursor c=db.rawQuery("SELECT titulo,quando FROM agenda WHERE id=?",new String[]{String.valueOf(editingId)})){if(c.moveToFirst()){title.setText(c.getString(0));selected.setTimeInMillis(c.getLong(1));}}}
 SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
 Button date=button("Data: "+fmt.format(selected.getTime()),()->{});
 date.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{selected.set(y,m,d);date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show());
 Button time=button("Horário: "+new SimpleDateFormat("HH:mm",new Locale("pt","BR")).format(selected.getTime()),()->{});
 time.setOnClickListener(v->new TimePickerDialog(this,(view,h,min)->{selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,min);time.setText(String.format(new Locale("pt","BR"),"Horário: %02d:%02d",h,min));date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());
 form.addView(date);form.addView(time);
 new AlertDialog.Builder(this).setTitle(editingId>=0?"Editar lembrete":"Novo lembrete").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Agendar",(d,w)->{
 String name=title.getText().toString().trim();long when=selected.getTimeInMillis();
 if(name.isEmpty()||when<=System.currentTimeMillis()){Toast.makeText(this,"Informe descrição e data futura",Toast.LENGTH_LONG).show();return;}
 ContentValues values=new ContentValues();values.put("titulo",name);values.put("quando",when);
 long id;
 if(editingId>=0){ReminderReceiver.cancel(this,editingId);id=db.update("agenda",values,"id=?",new String[]{String.valueOf(editingId)})>0?editingId:-1;}
 else id=db.insert("agenda",null,values);
 if(id>0){ReminderReceiver.schedule(this,(int)id,when);render();Toast.makeText(this,"Lembrete salvo. Ative as notificações.",Toast.LENGTH_LONG).show();}
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
 void entryForm(long editingId){
  boolean editing=editingId>=0;
  String oldType="Receita",oldDescription="",oldOrigin="",oldDetails="";long oldValue=0,oldDate=System.currentTimeMillis();
  if(editing){try(Cursor c=db.rawQuery("SELECT tipo,descricao,valor,data,origem,detalhes FROM lancamentos WHERE id=? AND area=?",new String[]{String.valueOf(editingId),area})){
   if(!c.moveToFirst())return;
   oldType=c.getString(0);oldDescription=c.getString(1);oldValue=c.getLong(2);oldDate=c.getLong(3);oldOrigin=c.getString(4);oldDetails=c.getString(5);
  }}
  Calendar selected=Calendar.getInstance();selected.setTimeInMillis(oldDate);
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(8),dp(18),dp(8));
  Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Receita","Despesa"}));type.setSelection(oldType.equals("Receita")?0:1);
  EditText description=new EditText(this);description.setSingleLine(true);description.setHint("Descrição / referência");description.setText(oldDescription);
  EditText amount=new EditText(this);amount.setHint("Valor (ex.: 350,50)");amount.setInputType(8194);if(editing)amount.setText(java.math.BigDecimal.valueOf(oldValue,2).toPlainString().replace('.',','));
  EditText origin=new EditText(this);origin.setSingleLine(true);origin.setHint("De quem ou de onde veio o dinheiro");origin.setText(oldOrigin==null?"":oldOrigin);
  EditText details=new EditText(this);details.setHint("Detalhes: a que se refere o recebimento");details.setMinLines(2);details.setMaxLines(4);details.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);details.setText(oldDetails==null?"":oldDetails);
  TextView originLabel=text("ORIGEM DO RECEBIMENTO",12,gold);
  TextView detailsLabel=text("DESCRIÇÃO DETALHADA",12,gold);
  TextView dateLabel=text("DATA E HORA DA ENTRADA",12,gold);
  SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
  Button date=button("Data: "+fmt.format(selected.getTime()),()->{});
  date.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{selected.set(y,m,d);date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show());
  Button time=button("Hora: "+new SimpleDateFormat("HH:mm",new Locale("pt","BR")).format(selected.getTime()),()->{});
  time.setOnClickListener(v->new TimePickerDialog(this,(view,h,min)->{selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,min);selected.set(Calendar.SECOND,0);time.setText(String.format(new Locale("pt","BR"),"Hora: %02d:%02d",h,min));date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());
  form.addView(text("TIPO DE LANÇAMENTO",12,gold));form.addView(type);form.addView(description);form.addView(amount);
  form.addView(originLabel);form.addView(origin);form.addView(detailsLabel);form.addView(details);
  form.addView(dateLabel);form.addView(date);form.addView(time);
  Runnable visibility=()->{boolean revenue=type.getSelectedItemPosition()==0;originLabel.setVisibility(revenue?View.VISIBLE:View.GONE);origin.setVisibility(revenue?View.VISIBLE:View.GONE);detailsLabel.setVisibility(revenue?View.VISIBLE:View.GONE);details.setVisibility(revenue?View.VISIBLE:View.GONE);};
  type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){visibility.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}});visibility.run();
  ScrollView scroll=new ScrollView(this);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(editing?"Editar lançamento — "+area:"Novo lançamento — "+area).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
  dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
   long cents;try{cents=parseCents(amount.getText().toString());}catch(Exception e){amount.setError("Informe um valor válido, ex.: 35,50");return;}
   String desc=description.getText().toString().trim();if(desc.isEmpty()){description.setError("Informe a descrição");return;}
   ContentValues cv=new ContentValues();cv.put("tipo",type.getSelectedItem().toString());cv.put("descricao",desc);cv.put("valor",cents);cv.put("data",selected.getTimeInMillis());
   cv.put("origem",type.getSelectedItemPosition()==0?origin.getText().toString().trim():"");cv.put("detalhes",type.getSelectedItemPosition()==0?details.getText().toString().trim():"");
   if(editing)db.update("lancamentos",cv,"id=? AND area=?",new String[]{String.valueOf(editingId),area});
   else{cv.put("area",area);cv.put("criado_em",System.currentTimeMillis());db.insertOrThrow("lancamentos",null,cv);}
   dialog.dismiss();render();
  }));dialog.show();
 }
 void editEntry(long id){entryForm(id);}
 void newEntry(){entryForm(-1);}

}