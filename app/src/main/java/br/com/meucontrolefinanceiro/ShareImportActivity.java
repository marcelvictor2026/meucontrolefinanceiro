package br.com.meucontrolefinanceiro;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

public class ShareImportActivity extends Activity {
 final int bg=Color.rgb(9,9,10), gold=Color.rgb(255,210,66);
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffaaa098);e.setSingleLine(true);return e;}
 TextView label(String t){TextView v=new TextView(this);v.setText(t);v.setTextColor(gold);v.setTextSize(13);v.setPadding(0,dp(12),0,0);return v;}
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
  Intent intent=getIntent();String raw=intent!=null&&(Intent.ACTION_SEND.equals(intent.getAction())&&"text/plain".equals(intent.getType())||"br.com.meucontrolefinanceiro.REVIEW_NOTIFICATION".equals(intent.getAction()))?intent.getStringExtra(Intent.EXTRA_TEXT):null;
  if(raw==null||raw.trim().isEmpty()){new AlertDialog.Builder(this).setMessage("Compartilhe uma mensagem de texto pelo WhatsApp.").setPositiveButton("OK",(d,w)->finish()).show();return;}
  if(raw.length()>12000)raw=raw.substring(0,12000);
  showConfirmation(raw);
 }
 void showConfirmation(String raw){
  String low=raw.toLowerCase(new Locale("pt","BR"));
  boolean expense=Pattern.compile("\\b(paguei|gastei|despesa|compra|pago|saída|saida)\\b").matcher(low).find();
  boolean revenue=Pattern.compile("\\b(recebi|recebido|receita|entrou|pagamento recebido|entrada)\\b").matcher(low).find();
  Matcher money=Pattern.compile("(?i)R\\$\\s*([0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)").matcher(raw);
  String amount="";boolean ambiguous=false;
  while(money.find()){if(!amount.isEmpty()){ambiguous=true;break;}amount=money.group(1);}
  if(amount.isEmpty()){
   Matcher decimal=Pattern.compile("(?<![\\d/:])([0-9]{1,3}(?:\\.[0-9]{3})+,[0-9]{2}|[0-9]+,[0-9]{2})(?![\\d/:])").matcher(raw);
   while(decimal.find()){if(!amount.isEmpty()){ambiguous=true;break;}amount=decimal.group(1);}
  }
  // Dates and other numbers can look like money. User must always verify.
  Calendar selected=Calendar.getInstance();
  if(low.contains("ontem"))selected.add(Calendar.DAY_OF_MONTH,-1);
  Matcher fullDate=Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b").matcher(raw);
  if(fullDate.find()){int day=Integer.parseInt(fullDate.group(1)),month=Integer.parseInt(fullDate.group(2)),year=Integer.parseInt(fullDate.group(3));if(month>=1&&month<=12&&day>=1&&day<=31)selected.set(year,month-1,day);}
  Matcher hour=Pattern.compile("\\b(?:às|as|hora:?|horário:?)?\\s*(\\d{1,2})[:h](\\d{2})?\\b",Pattern.CASE_INSENSITIVE).matcher(raw);
  if(hour.find()){int h=Integer.parseInt(hour.group(1)),m=hour.group(2)==null?0:Integer.parseInt(hour.group(2));if(h<24&&m<60)selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,m);}
  selected.set(Calendar.SECOND,0);selected.set(Calendar.MILLISECOND,0);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(22),dp(20),dp(28));scroll.addView(root);
  TextView title=label("IMPORTAR DO WHATSAPP");title.setTextSize(21);root.addView(title);
  TextView info=label("Confira e edite todos os campos antes de salvar. Nenhum lançamento é registrado automaticamente.");info.setTextColor(Color.LTGRAY);root.addView(info);
  root.addView(label("MENSAGEM RECEBIDA"));
  TextView message=new TextView(this);message.setText(raw);message.setTextColor(Color.WHITE);message.setTextSize(14);message.setPadding(dp(12),dp(12),dp(12),dp(12));message.setBackgroundColor(0xff24201b);root.addView(message);
  root.addView(label("ÁREA"));Spinner area=new Spinner(this);area.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Pessoal","Empresa"}));root.addView(area);
  root.addView(label("TIPO"));Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Receita","Despesa"}));type.setSelection(expense&&!revenue?1:0);root.addView(type);
  root.addView(label("VALOR (R$)"));EditText value=field("Ex.: 1500,00",ambiguous?"":amount);value.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);root.addView(value);
  if(ambiguous||amount.isEmpty()){TextView warn=label("Valor não identificado com segurança. Preencha manualmente.");warn.setTextColor(0xffffad55);root.addView(warn);}
  root.addView(label("DESCRIÇÃO"));EditText description=field("A que se refere", "Movimentação informada pelo WhatsApp");root.addView(description);
  root.addView(label("ORIGEM / QUEM PAGOU"));EditText origin=field("Cliente, empresa ou pessoa",getIntent().getStringExtra("sender")==null?"":getIntent().getStringExtra("sender"));root.addView(origin);
  root.addView(label("DETALHES"));EditText details=field("Informações adicionais",raw);details.setSingleLine(false);details.setMinLines(3);root.addView(details);
  root.addView(label("DATA E HORA DA MOVIMENTAÇÃO"));
  SimpleDateFormat fmt=new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR"));
  Button date=new Button(this);date.setAllCaps(false);date.setText("Data: "+fmt.format(selected.getTime()));root.addView(date);
  date.setOnClickListener(v->new DatePickerDialog(this,(picker,y,m,d)->{selected.set(y,m,d);date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show());
  Button time=new Button(this);time.setAllCaps(false);time.setText("Hora: "+new SimpleDateFormat("HH:mm",new Locale("pt","BR")).format(selected.getTime()));root.addView(time);
  time.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->{selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,m);time.setText(String.format(new Locale("pt","BR"),"Hora: %02d:%02d",h,m));date.setText("Data: "+fmt.format(selected.getTime()));},selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());
  Button save=new Button(this);save.setAllCaps(false);save.setText("CONFIRMAR E SALVAR");save.setTextColor(Color.BLACK);GradientDrawable grad=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{gold,0xffff9628,0xffe35b15});grad.setCornerRadius(dp(18));save.setBackground(grad);root.addView(save);
  Button cancel=new Button(this);cancel.setText("Cancelar");cancel.setAllCaps(false);root.addView(cancel);cancel.setOnClickListener(v->finish());
  save.setOnClickListener(v->{long cents;try{String num=value.getText().toString().trim().replace("R$","").replace(" ","");if(num.contains(","))num=num.replace(".","").replace(",",".");cents=new java.math.BigDecimal(num).movePointRight(2).longValueExact();if(cents<=0)throw new Exception();}catch(Exception e){value.setError("Informe um valor válido");return;}
   String desc=description.getText().toString().trim();if(desc.isEmpty()){description.setError("Informe a descrição");return;}
   String chosenArea=area.getSelectedItem().toString(),chosenType=type.getSelectedItem().toString();
   new AlertDialog.Builder(this).setTitle("Confirmar lançamento").setMessage(chosenArea+" • "+chosenType+"\nR$ "+value.getText()+"\n"+desc+"\n"+fmt.format(selected.getTime())+"\n\nSalvar no controle financeiro?")
   .setNegativeButton("Revisar",null).setPositiveButton("Salvar",(d,w)->{
    try(SQLiteDatabase db=openOrCreateDatabase("financas.db",MODE_PRIVATE,null)){
     db.execSQL("CREATE TABLE IF NOT EXISTS lancamentos(id INTEGER PRIMARY KEY,area TEXT,tipo TEXT,descricao TEXT,valor INTEGER,data INTEGER)");
     for(String col:new String[]{"origem TEXT DEFAULT ''","detalhes TEXT DEFAULT ''","criado_em INTEGER DEFAULT 0"})try{db.execSQL("ALTER TABLE lancamentos ADD COLUMN "+col);}catch(android.database.SQLException ignored){}
     ContentValues cv=new ContentValues();cv.put("area",chosenArea);cv.put("tipo",chosenType);cv.put("descricao",desc);cv.put("valor",cents);cv.put("data",selected.getTimeInMillis());cv.put("origem",origin.getText().toString().trim());cv.put("detalhes",details.getText().toString().trim());cv.put("criado_em",System.currentTimeMillis());
     if(db.insertOrThrow("lancamentos",null,cv)<0)throw new Exception("Falha ao gravar");
     Toast.makeText(this,"Lançamento salvo em "+chosenArea,Toast.LENGTH_LONG).show();finish();
    }catch(Exception e){new AlertDialog.Builder(this).setMessage("Não foi possível salvar: "+e.getMessage()).setPositiveButton("OK",null).show();}
   }).show();
  });
  setContentView(scroll);
 }
}