/*Copyright (c) 2010, Parallels-NSU lab. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided 
that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions 
    * and the following disclaimer.
    
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
    * and the following disclaimer in the documentation and/or other materials provided with 
    * the distribution.
    
    * Neither the name of the Parallels-NSU lab nor the names of its contributors may be used to endorse 
    * or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package hpi;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ScriptSelectionTaskDefinition extends SimpleParameterDefinition {
	private static final long serialVersionUID = 1L;
	//-------------------------------------------------------------------------
	private       String            defaultValue    = "";
	private       String            testDelimSymbol = "~";
	private       String            nodeDelimSymbol = ".";
	private       String            path;
    private       int               countDelimiterSymbol = 2;
    private       String            delimiter = " ";
    //-------------------------------------------------------------------------
    private       int               deep = 0;
    private       ArrayCheckBox[]   arrayCheckBox;
	//-------------------------------------------------------------------------
    private final static String SECRET_KEY = "__ttp__sk__";
	//-------------------------------------------------------------------------        
    @DataBoundConstructor
    public ScriptSelectionTaskDefinition(String name, String path, ArrayCheckBox[] arrayCheckBox, 
    									 String testDelimSymbol, String nodeDelimSymbol, String description, 
    									 int countDelimiterSymbol, String delimiter, String defaultValue) {
        super(name, description);
        this.path                 = path;
        this.testDelimSymbol      = testDelimSymbol;
        this.nodeDelimSymbol       = nodeDelimSymbol;
        this.countDelimiterSymbol = countDelimiterSymbol;
        this.delimiter            = delimiter;
        this.arrayCheckBox        = arrayCheckBox;
        this.defaultValue         = defaultValue;
    }
    //-------------------------------------------------------------------------
    public synchronized String getPath() {
        return(this.path);
    }
    public synchronized String getTestDelimSymbol() {
    	return(this.testDelimSymbol);
    }
    public synchronized String getNodeDelimSymbol() {
    	return(this.nodeDelimSymbol);
    }
    public synchronized int getCountDelimiterSymbol() {
    	return(this.countDelimiterSymbol);
    }
    public synchronized String getDelimiter() {
    	return(this.delimiter);
    }
    public synchronized String getDefaultValue() {
    	return(this.defaultValue);
    }
    public synchronized String getSaveScriptFile() {
		String s = "";
		String home = Hudson.getInstance().getRootDir().toString();
		if(home!=null){
			int pos = this.path.length()-1;
			while(pos>=0) {
				if(this.path.getBytes()[pos]==File.separator.toCharArray()[0]) {
					break;
				}
				pos--;
			}
			String path = home + File.separator + "jobs" + File.separator;
			if(pos>0) path += this.path.substring(0, pos);    			
			String exe = this.path.substring(pos+1);
			try {
				File currDir = new File(path);
				if(currDir.exists()) {
					String[] command = new String[1];
					command[0] = path + File.separator + exe;
    				Process proc = Runtime.getRuntime().exec(command,null,currDir);
    				BufferedInputStream data = new BufferedInputStream(proc.getInputStream());
					try {
    					while(true) {
    						byte[] bufData = new byte[4096];
    						int re = data.read(bufData);
    						if(re==4096) {
    							s+=(new String(bufData));
    						} else {
    							if(re > 0) {
    								s+=(new String(bufData,0,re));
    							} else if(re <= 0) {
    								break;
    							}
    						}
    					}
					} catch (EOFException ex) {
						s = ex.getMessage();
					} catch (IOException ex) {
						s = ex.getMessage();
					} catch (RuntimeException ex) {
						s = ex.getMessage();
					} finally {
						if(proc!=null) proc.destroy();
					}
				} else {
					s += "Error : Directory = " + currDir.toString() + " doesn't found";
				}
			} catch (Exception ex) {
				s = ex.getMessage();
			}
		}
    	return(s);
    }
    @Override
    public synchronized ParameterValue createValue(StaplerRequest req, JSONObject jo) {
    	String valueResult = new String("");
        //---------------------------------------
    	if(this.arrayCheckBox==null || (this.arrayCheckBox.length==1 && this.arrayCheckBox[0].type.equals("~"))) {
			StringParameterValue value = new StringParameterValue(getName(), valueResult);
	    	value.setDescription(getDescription());
	        return value;	
		}
		int currDeep = 0;
    	for(int i = 0 ; i < this.arrayCheckBox.length ; i++) {
    		if(this.arrayCheckBox[i].type.equals("[")) {
    			currDeep++;
    			if((Boolean)jo.get(this.arrayCheckBox[i+1].variableName)==false) {
	    			i++;
    				int bufDeep = currDeep;
	    			while(bufDeep>=currDeep) {
	    				if(this.arrayCheckBox[i+1].type.equals("["))
	    					bufDeep++;
	    				if(this.arrayCheckBox[i+1].type.equals("]"))
	    					bufDeep--;
	    				i++;
	    			}
	    			currDeep--;
    			}
    		} else if(this.arrayCheckBox[i].type.equals("]")) {
    			currDeep--;
    		} else if(this.arrayCheckBox[i].type.equals("L")){
				valueResult += (this.testDelimSymbol + this.arrayCheckBox[i].parent + this.nodeDelimSymbol + this.arrayCheckBox[i].name);
    		}
    	}
        //---------------------------------------
    	StringParameterValue value = new StringParameterValue(getName(), valueResult);
    	value.setDescription(getDescription());
        return value;
    }
    public synchronized ParameterValue createValue(String value) {
    	return new StringParameterValue(getName(),String.valueOf(value),getDescription());
    }
    @Override
    public synchronized StringParameterValue getDefaultParameterValue() {
        return new StringParameterValue(getName(), this.defaultValue, getDescription());
    }
//-----------------------------------------------------------------------------
//Descriptor
//-----------------------------------------------------------------------------
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Script Selection task variable";
        }

        public FormValidation doCheckPath(@QueryParameter String value) throws IOException, ServletException {
        	String home = Hudson.getInstance().getRootDir().toString();
        	if(home==null) {
    			return FormValidation.error("Sorry, HUDSON_HOME variable not found. Contact the server administrator, please.");
    		} else {
    			int pos = value.length() - 1;
    			while(pos>=0) {
    				if(value.getBytes()[pos]==File.separator.toCharArray()[0]) {
    					break;
    				}
    				pos--;
    			}
    			String path = home + File.separator + "jobs" + File.separator;
    			if(pos>0) path += value.substring(0, pos);
    			String exe = value.substring(pos+1);
    			try {
    				File currDir = new File(path);
    				if(currDir.exists() && currDir.isDirectory()) {
    					String[] command = new String[1];
    					command[0] = path + File.separator + exe;
        				Process proc = Runtime.getRuntime().exec(command,null,currDir);
        				if(proc!=null) proc.destroy();
    				} else {
    					return FormValidation.error("Directory " + path + " not found");
    				}
    			} catch (Exception ex) {
    				return FormValidation.error(ex.getMessage());
    			}
    		}
        	return FormValidation.ok();
        }
        public FormValidation doCheckCountDelimiterSymbol(@QueryParameter String value) throws IOException, ServletException {
        	try {
        		Integer i = new Integer(value);
        		if(i<=0) {
        			return(FormValidation.error("Variable must be > 0"));
        		}
        	} catch (Exception ex) {
        		return(FormValidation.error("It is not integer"));
        	}
        	
        	return(FormValidation.ok());
        }
        public FormValidation doCheckDelimiter(@QueryParameter String value) throws IOException, ServletException {
        	if(value.length()!=1)
        		return(FormValidation.error("Length must be 1"));
        	
        	return(FormValidation.ok());
        }
    }
    //-------------------------------------------------------------------------
    public synchronized String getCurrentOffsetOpen() {
    	String s = new String();
    	
    	for (int i = 0 ; i < this.deep ; i++) {
    		s+="<ul>";
    	}
    	return(s);
    }
    public synchronized String getCurrentOffsetClose() {
    	String s = new String();
    	
    	for (int i = 0 ; i < this.deep ; i++) {
    		s+="</ul>";
    	}
    	return(s);
    }
    public synchronized int plusDeep() {
    	this.deep++;
    	return(this.deep);
    }
    public synchronized int minusDeep() {
    	this.deep--;
    	return(this.deep);
    }
    public synchronized ArrayCheckBox[] getExpr() {
		String home = Hudson.getInstance().getRootDir().toString();
		if(home==null) {
			this.arrayCheckBox = new ArrayCheckBox[1];
			this.arrayCheckBox[0] = new ArrayCheckBox("Error : Sorry, HUDSON_HOME variable not found. Contact the server administrator, please.", "no_variable", true, "~", "");
		} else {
			int pos = this.path.length()-1;
			while(pos>=0) {
				if(this.path.getBytes()[pos]==File.separator.toCharArray()[0]) {
					break;
				}
				pos--;
			}
			String path = home + File.separator + "jobs" + File.separator;
			if(pos>0) path += this.path.substring(0, pos);    			
			String exe = this.path.substring(pos+1);
			
			try {
				File currDir = new File(path);
				if(currDir.exists()) {
					String[] command = new String[1];
					command[0] = path + File.separator + exe;
    				Process proc = Runtime.getRuntime().exec(command,null,currDir);
    				BufferedInputStream data = new BufferedInputStream(proc.getInputStream());
					String s = "";
					try {
    					while(true) {
    						byte[] bufData = new byte[4096];
    						int re = data.read(bufData);
    						if(re==4096) {
    							s+=(new String(bufData));
    						} else {
    							if(re > 0) {
    								s+=(new String(bufData,0,re));
    							} else if(re <= 0) {
    								break;
    							}
    						}
    					}
    					Directory root = new Directory("");
    					try {
    						if(this.delimiter.length()==1) {
    							recursionFromString(s, 0, root,this.countDelimiterSymbol,this.delimiter.getBytes()[0]);
	    						Queue<ArrayCheckBox>queue = new ArrayDeque<ArrayCheckBox>();
        				    	//-----------------------
        				        //парсим входной параметр----------------
        				        String[] arrayNodes;
        				        String bufDefaultValue = this.defaultValue;
        				        String bufStr = "";
        				        Queue<String> queueDefault = new ArrayDeque<String>();
        				        while(bufDefaultValue.length()>0) {
        				        	if(bufDefaultValue.indexOf(this.testDelimSymbol)<0) {
        				        		queueDefault.add(bufDefaultValue);
        				        		break;
        				        	}
        				        	if(bufDefaultValue.indexOf(this.testDelimSymbol)==0) {
        				        		bufDefaultValue = bufDefaultValue.substring(this.testDelimSymbol.length());
        				        		queueDefault.add(bufStr);
        				        		bufStr = "";
        				        	} else {
        				        		byte[] b = new byte[1];
        				        		b[0] = bufDefaultValue.getBytes()[0];
        				        		bufStr +=  new String(b);
        				        		bufDefaultValue = bufDefaultValue.substring(1);
        				        	}
        				        }
        				        arrayNodes = new String[queueDefault.size()];
        				        int i = 0;
        				        for(String buf : queueDefault) {
        				        	arrayNodes[i] = buf;
        				        	i++;
        				        }
        				    	//-----------------------
        				    	recursion(root, queue,"",this.nodeDelimSymbol,arrayNodes);
        				    	this.arrayCheckBox = new ArrayCheckBox[queue.size()-3];
        				    		    	
        				    	Collection<ArrayCheckBox>list = queue;
        				    	i = 0;
        				    	for(ArrayCheckBox buf : list) {
        				    		if(i>=2 && i<queue.size()-1)
        				    			arrayCheckBox[i - 2] = new ArrayCheckBox(buf.name,SECRET_KEY+"var_"+(i-2),buf.check,buf.type,buf.parent);
        				    		i++;
        				    	}
    						} else {
	    	        			this.arrayCheckBox = new ArrayCheckBox[1];
	    	        			this.arrayCheckBox[0] = new ArrayCheckBox("Error : Space symbol in script file must be single(Example = ' ')", "no_variable", true, "~", "");	    							
    						}
    					} catch (Exception ex) {
    	        			this.arrayCheckBox = new ArrayCheckBox[1];
    	        			this.arrayCheckBox[0] = new ArrayCheckBox(ex.getMessage(), "no_variable", true, "~", "");						
    					}
					} catch (EOFException ex) {
						this.arrayCheckBox = new ArrayCheckBox[1];
	        			this.arrayCheckBox[0] = new ArrayCheckBox("Error : EOFException("+ex.getMessage()+")", "no_variable", true, "~", "");						
					} catch (IOException ex) {
						this.arrayCheckBox = new ArrayCheckBox[1];
	        			this.arrayCheckBox[0] = new ArrayCheckBox("Error : IOException("+ex.getMessage()+")", "no_variable", true, "~", "");    				
					} catch (RuntimeException ex) {
						this.arrayCheckBox = new ArrayCheckBox[1];
	        			this.arrayCheckBox[0] = new ArrayCheckBox("Error : RuntimeException("+ex.getMessage()+").", "no_variable", true, "~", ""); 						
					} finally {
						if(proc!=null) proc.destroy();
					}
				} else {
					this.arrayCheckBox = new ArrayCheckBox[1];
        			this.arrayCheckBox[0] = new ArrayCheckBox("Error : Directory = " + path + " does not found", "no_variable", true, "~", "");     					
				}
			} catch (Exception ex) {
    			this.arrayCheckBox = new ArrayCheckBox[1];
    			this.arrayCheckBox[0] = new ArrayCheckBox("Error : " + ex.getMessage(), "no_variable", true, "~", "");    				
			}
		}
    	return(this.arrayCheckBox);
    }
    public synchronized void setDefaultValue(String defaultValue) {
    	this.defaultValue = defaultValue;
    }
    public static void main(String[] args) {
    	byte[] b = new byte[1];
    	b[0] = '\n';
    	String del = new String(b);
    	String s = "Node1" + del + 
    	           "  2" + del + del + del + 
    	           "  3" + del + 
    	           "Node2"+del+del+
    	           "  3" + del +
    	           "  4" + del + del;
    	Directory dir = new Directory("");
    	try {
    		String[] arrayNodes = new String[0];
    		recursionFromString(s, 0, dir, 2, (byte)' ');
    		Queue<ArrayCheckBox> queue = new ArrayDeque<ArrayCheckBox>();
    		recursion(dir, queue, "", "~", arrayNodes);
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
//*****************************************************************************
//Static methods
//*****************************************************************************
    private static int calcSpace(int norm, String s, byte delimiter) {
    	int count = 0;
    	for(int i = 0 ; i < s.length() ; i++) {
    		if (s.getBytes()[i]==delimiter) {
    			count++;
    		} else {
    			break;
    		}
    	}
    	if ((count%norm)==0) return(count/norm);
    	return(-1);
    }
    private static String skipSpace(String s, byte delimiter) {
    	int pos = 0;
    	for(int i = 0 ; i < s.length() ; i++) {
    		if(s.getBytes()[i]==delimiter) {
    			pos++;
    		} else {
    			break;
    		}
    	}
    	if(pos==s.length()) return(null);
    	return(s.substring(pos));
    }
    private static String getLine(String s) {
    	int i = 0;
    	
    	while(i<s.length()) {
    		if(s.getBytes()[i]=='\n' || (i+1<s.length()&& s.getBytes()[i]=='\r' && s.getBytes()[i+1]=='\n')) {
   				return(s.substring(0, i));
    		}
    		i++;
    	}
    	return("");
    }
    private static String recursionFromString(String input, int deep, Directory dir, int countSpaceSymbol, byte delimiter) throws Exception {
    	Directory  currDirectory = null;
    	while(input.length()!=0) {
	    	String saveStr = new String(input);
    		String s = getLine(input);
	    	int i = 0;
    		for(i = 0 ; i < s.length() ; i++) {
	    		if(!(s.getBytes()[i]==' ' || s.getBytes()[i]=='\r' || s.getBytes()[i]=='\n')) {
	    			break;
	    		}
	    	}
    		if(input.length()>s.length()) {
	    		if(input.getBytes()[s.length()]=='\n') {
	    			input = input.substring(s.length()+1);
	    		} else if(input.length()>s.length()+1) {
		    		if(input.getBytes()[s.length()]=='\r' && input.getBytes()[s.length()+1]=='\n') {
		    			input = input.substring(s.length()+2);
		    		} else {
		    			input = "";
		    		}
	    		} else {
	    			input = "";
	    		}
	    	} else {
	    		input = "";
	    	}
	    	if(i==s.length()) {
	    		if(input.length()==0)
	    			return("");
	    		continue;
	    	}
	    	//-----------------------------------
	    	int re = calcSpace(countSpaceSymbol, s, delimiter);
	    	if(re==-1) {
				throw new Exception("Error : Everyone string of file doesn't have length = 0.");	    		
	    	}
	    	if(re==deep) {
	    		String buf = skipSpace(s.substring(re*countSpaceSymbol),delimiter); 
    			if(buf==null) {
	    			throw new Exception("Error : Everyone node must have name(Str = " + input + ").");
    			}
    			currDirectory = new Directory(buf);
    			dir.addDirectory(currDirectory);
	    	} else if(re<deep) {
	    		input = saveStr;
	    		return(input);
	    	} else if(re>deep) {
	    		if((re)*countSpaceSymbol!=(deep+1)*countSpaceSymbol) return(null);
	    		String buf = skipSpace(s.substring(re*countSpaceSymbol),delimiter); 
	    		if(buf==null) return(null);
    			if(currDirectory==null) {
    				throw new Exception("Error : Class or directory = " + skipSpace(buf.substring(1),delimiter) + " hasn't parent directory.");
    			}
    			input = recursionFromString(saveStr, deep + 1, currDirectory,countSpaceSymbol,delimiter);
	    	}
    	}
    	return("");
    }
    private static void recursion(Directory dir, Queue<ArrayCheckBox>queue, String parent, String dirDelimSymb, String[] arrayNodes) {
    	if(dir!=null && dir.getName()!=null) {
    		queue.add(new ArrayCheckBox("[", "", true, "[",""));
    		if(dir.getNested().size()==0) {
    			//сравнение------------------
    			boolean checkTask = false;
    			for(int i = 0 ; i < arrayNodes.length ; i++) {
    				if((parent + dirDelimSymb + dir.getName()).equals(arrayNodes[i])) {
    					checkTask = true;
    					break;
    				}
    			}
				if(arrayNodes.length==0)
					checkTask = true;
				//---------------------------
    			queue.add(new ArrayCheckBox(dir.getName(), "", checkTask, "L",parent));
    		} else {
    			queue.add(new ArrayCheckBox(dir.getName(), "", true, "R",parent));
    		}
    	}
    	Collection<Directory>listDir = dir.getNested();
    	for(Directory bufDir : listDir) {
			if(dir.getName()==null || dir.getName().equals("")) {
    			recursion(bufDir, queue, "", dirDelimSymb, arrayNodes);
			} else {
				if(!parent.equals("")) {
					recursion(bufDir, queue, parent + dirDelimSymb + dir.getName(), dirDelimSymb, arrayNodes);
				} else {
					recursion(bufDir, queue, dir.getName(), dirDelimSymb, arrayNodes);
				}
			}
    	}
    	if(dir!=null && dir.getName()!=null) {
    		queue.add(new ArrayCheckBox("]", "", true, "]",""));
    	}
    }
}

