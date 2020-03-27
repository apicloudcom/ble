package com.apicloud.uzble;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;

import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
import com.uzmap.pkg.uzkit.UZUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by someone on 2018/5/10.
 */

public class MouleUtil {
    public static void paramterError(UZModuleContext uzModuleContext) {
    	if (uzModuleContext == null) {
			return;
		}
        JSONObject jObject=new JSONObject();
        try {
            jObject.put("status", false);
            jObject.put("msg", "parameter error");
            uzModuleContext.error(jObject, jObject, false);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void error(UZModuleContext uzModuleContext, String message) {
    	if (uzModuleContext == null) {
			return;
		}
        JSONObject jObject=new JSONObject();
        try {
            jObject.put("status", false);
            if (!TextUtils.isEmpty(message)) {
            	jObject.put("msg", message+"");
			}
            uzModuleContext.error(jObject, jObject, false);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void succes(UZModuleContext uzModuleContext){
    	if (uzModuleContext == null) {
			return;
		}
        JSONObject jObject=new JSONObject();
        try {
            jObject.put("status", true);
            uzModuleContext.success(jObject,false);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void succes(UZModuleContext uzModuleContext, HashMap<String,Object> parameter, boolean sign){
    	if (uzModuleContext == null) {
			return;
		}
        JSONObject jObject=new JSONObject();
        try {
        	if (sign) {
				
        		jObject.put("status", true);
			}
            if (parameter!=null && parameter.size()>0){
                for (String key:parameter.keySet()) {
                    jObject.put(key,parameter.get(key));
                }
            }
            uzModuleContext.success(jObject,false);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void error(UZModuleContext uzModuleContext, HashMap<String,Object> parameter, boolean sign){
    	if (uzModuleContext == null) {
			return;
		}
        JSONObject jObject=new JSONObject();
        JSONObject ret=new JSONObject();
        try {
        	ret.put("status", false);
        	if (sign) {
        		jObject.put("status", false);
			}
            if (parameter!=null && parameter.size()>0){
                for (String key:parameter.keySet()) {
                    jObject.put(key,parameter.get(key));
                    ret.put(key, parameter.get(key));
                }
            }
            uzModuleContext.error(ret, jObject, false);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    
    public static  Bitmap getBitmap(String path) {
    	if (TextUtils.isEmpty(path)) {
			return null;
		}
		Bitmap bitmap = null;
		InputStream input = null;
		try {
			input = UZUtility.guessInputStream(path);
			bitmap = BitmapFactory.decodeStream(input);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}

	public static  String saveBitmap(Bitmap bitmap,String fileName) {
		String result = null;
		if (bitmap == null) {
			return null;
		}
		ByteArrayOutputStream baos = null;
		try {
			if (bitmap != null) {
				baos = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

				baos.flush();
				baos.close();

				byte[] bitmapBytes = baos.toByteArray();
				result = saveFile(bitmapBytes, fileName+"_apicloud.png");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
    
    public static  String saveBitmap(Bitmap bitmap) {
    	String result = null;
    	if (bitmap == null) {
			return null;
		}
    	ByteArrayOutputStream baos = null;
		try {
			if (bitmap != null) {
				baos = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

				baos.flush();
				baos.close();

				byte[] bitmapBytes = baos.toByteArray();
				result = saveFile(bitmapBytes, System.currentTimeMillis()+"_apicloud.png");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
    
    //保存文件
	public static String saveFile(byte[] content,String fileName) {
    	File file = new File(UZUtility.getExternalCacheDir(),fileName);
//		File file = new File(UZUtility.getExternaStoragePath(),fileName);
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(content);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
            return null;
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();

	}
	
	//保存文件
		public static String appendFile(byte[] content,String fileName) {
	    	File file = new File(UZUtility.getExternalCacheDir(),fileName);
	        // 创建FileOutputStream对象
	        FileOutputStream outputStream = null;
	        // 创建BufferedOutputStream对象
	        BufferedOutputStream bufferedOutputStream = null;
	        try {
	            // 如果文件存在则删除
	            if (file.exists()) {
//	                file.delete();
	            }else {
	            	 // 在文件系统中根据路径创建一个新的空文件
	            	file.createNewFile();
				}
	           
	            // 获取FileOutputStream对象
	            outputStream = new FileOutputStream(file,true);
	            // 获取BufferedOutputStream对象
	            bufferedOutputStream = new BufferedOutputStream(outputStream);
	            // 往文件所在的缓冲输出流中写byte数据
	            bufferedOutputStream.write(content);
	            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
	            bufferedOutputStream.flush();
	        } catch (Exception e) {
	            // 打印异常信息
	            e.printStackTrace();
	            return null;
	        } finally {
	            // 关闭创建的流对象
	            if (outputStream != null) {
	                try {
	                    outputStream.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	            if (bufferedOutputStream != null) {
	                try {
	                    bufferedOutputStream.close();
	                } catch (Exception e2) {
	                    e2.printStackTrace();
	                }
	            }
	        }
	        return file.getAbsolutePath();

		}
	
	public static void inputstreamtofile(InputStream ins,File file){
    	OutputStream os;
		try {
			os = new FileOutputStream(file);
			int bytesRead = 0;
	    	byte[] buffer = new byte[8192];
	    	while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
	    	os.write(buffer, 0, bytesRead);
	    	}
	    	os.close();
	    	ins.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	}
	
	//缩放bitmap
	public static Bitmap imageZoom(Bitmap bitmap,double maxSize) {
        //将bitmap放至数组中，意在bitmap的大小（与实际读取的原文件要大）  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        //将字节换成KB
        double mid = b.length/1024;
        //判断bitmap占用空间是否大于允许最大空间  如果大于则压缩 小于则不压缩
        if (mid > maxSize) {
                //获取bitmap大小 是允许最大大小的多少倍
                double i = mid / maxSize;
                //开始压缩  此处用到平方根 将宽带和高度压缩掉对应的平方根倍 （1.保持刻度和高度和原bitmap比率一致，压缩后也达到了最大大小占用空间的大小）
                return zoomImage(bitmap, bitmap.getWidth() / Math.sqrt(i),
                		bitmap.getHeight() / Math.sqrt(i));
        }
        return bitmap;
}



/***
 * 图片的缩放方法
 * 
 * @param bgimage
 *            ：源图片资源
 * @param newWidth
 *            ：缩放后宽度
 * @param newHeight
 *            ：缩放后高度
 * @return
 */
public static Bitmap zoomImage(Bitmap bgimage, double newWidth,
                double newHeight) {
        // 获取这个图片的宽和高
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
                        (int) height, matrix, true);
        return bitmap;
}


public static String getReadPath(UZModule uzModule, String url) {
	if (url!=null && url.startsWith("widget") && url.contains("/") ) {
		String fileName = url.substring(url.lastIndexOf("/")+1);
		String filePath = UZUtility.getExternalCacheDir()+fileName;
		try {
			inputstreamtofile(UZUtility.guessInputStream(uzModule.makeRealPath(url)), new File(filePath));
			return filePath;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	return uzModule.makeRealPath(url);
}

public static byte[] toByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
        output.write(buffer, 0, n);
    }
    return output.toByteArray();
}

public static byte[] bitmapToByte(Bitmap bmp){
	if (bmp == null){
		return null;
	}
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
	return baos.toByteArray();
}

public static Bitmap byteToBitmap(byte[] data){
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inSampleSize = 1;
	return BitmapFactory.decodeByteArray(data, 0, data.length);
}

	public static String readFile(String filePath) throws IOException {
		StringBuffer sb = new StringBuffer();
		readToBuffer(sb, filePath);
		return sb.toString();
	}

	public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
		InputStream is = new FileInputStream(filePath);
		String line; // 用来保存每行读取的内容
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		line = reader.readLine(); // 读取第一行
		while (line != null) { // 如果 line 为空说明读完了
			buffer.append(line); // 将读到的内容添加到 buffer 中
			buffer.append("\n"); // 添加换行符
			line = reader.readLine(); // 读取下一行
		}
		reader.close();
		is.close();
	}
}
