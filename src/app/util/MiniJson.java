package app.util;

import java.util.*;

public final class MiniJson {
    private MiniJson(){}
    public static Object parse(String text){Parser p=new Parser(text);Object v=p.value();p.ws();if(p.i!=text.length())throw new IllegalArgumentException("Unexpected JSON content at "+p.i);return v;}
    public static String stringify(Object value){StringBuilder b=new StringBuilder();write(value,b);return b.toString();}
    private static void write(Object v,StringBuilder b){
        if(v==null){b.append("null");return;}
        if(v instanceof String){b.append('"').append(((String)v).replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")).append('"');return;}
        if(v instanceof Number || v instanceof Boolean){b.append(v);return;}
        if(v instanceof Map){b.append('{');boolean first=true;for(Object e0:((Map<?,?>)v).entrySet()){Map.Entry<?,?> e=(Map.Entry<?,?>)e0;if(!first)b.append(',');first=false;write(String.valueOf(e.getKey()),b);b.append(':');write(e.getValue(),b);}b.append('}');return;}
        if(v instanceof Iterable){b.append('[');boolean first=true;for(Object x:(Iterable<?>)v){if(!first)b.append(',');first=false;write(x,b);}b.append(']');return;}
        write(String.valueOf(v),b);
    }
    private static class Parser {
        final String s;int i;Parser(String s){if(s==null)throw new IllegalArgumentException("JSON is null");this.s=s;}
        void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        Object value(){ws();if(i>=s.length())throw err();char c=s.charAt(i);if(c=='{')return object();if(c=='[')return array();if(c=='"')return string();if(c=='t'){literal("true");return true;}if(c=='f'){literal("false");return false;}if(c=='n'){literal("null");return null;}return number();}
        Map<String,Object> object(){i++;Map<String,Object> m=new LinkedHashMap<>();ws();if(take('}'))return m;while(true){ws();String k=string();ws();need(':');m.put(k,value());ws();if(take('}'))return m;need(',');}}
        List<Object> array(){i++;List<Object> a=new ArrayList<>();ws();if(take(']'))return a;while(true){a.add(value());ws();if(take(']'))return a;need(',');}}
        String string(){need('"');StringBuilder b=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){if(i>=s.length())throw err();char e=s.charAt(i++);switch(e){case '"':case '\\':case '/':b.append(e);break;case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;case 'u':if(i+4>s.length())throw err();b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;break;default:throw err();}}else b.append(c);}throw err();}
        Number number(){int st=i;if(i<s.length()&&s.charAt(i)=='-')i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;if(i<s.length()&&s.charAt(i)=='.'){i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}if(st==i)throw err();String n=s.substring(st,i);try{return n.contains(".")||n.contains("e")||n.contains("E")?Double.parseDouble(n):Long.parseLong(n);}catch(Exception e){throw err();}}
        void literal(String x){if(!s.startsWith(x,i))throw err();i+=x.length();}
        boolean take(char c){if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
        void need(char c){ws();if(!take(c))throw err();}
        IllegalArgumentException err(){return new IllegalArgumentException("Invalid JSON at position "+i);}
    }
}
