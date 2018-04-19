/*******************************************************************************
 * Copyright (c) 2009, 2016 GreenVulcano ESB Open Source Project.
 * All rights reserved.
 *
 * This file is part of GreenVulcano ESB.
 *
 * GreenVulcano ESB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GreenVulcano ESB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GreenVulcano ESB. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package it.greenvulcano.gvesb.virtual.qrcode;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.virtual.CallException;
import it.greenvulcano.gvesb.virtual.CallOperation;
import it.greenvulcano.gvesb.virtual.ConnectionException;
import it.greenvulcano.gvesb.virtual.InitializationException;
import it.greenvulcano.gvesb.virtual.InvalidDataException;
import it.greenvulcano.gvesb.virtual.OperationKey;
import it.greenvulcano.util.metadata.PropertiesHandler;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.w3c.dom.Node;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;


/**
 * 
 * @version 4.0 03/april/2018
 * @author GreenVulcano Developer Team
 */
public class QrEncodeCallOperation implements CallOperation {
    
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(QrEncodeCallOperation.class); 
    
    private static final String TEXT_FONT_NAME ="Monospace";
    private static final int TEXT_FONT_SIZE = 48;
    
    /*
     * Sizes computed for font Monospace - size 48
     */
    private static final int TEXT_CHARACTER_WITH = 32;
    private static final int TEXT_HORIZONTAL_MARGIN = 48;
    private static final int TEXT_VERTICAL_MARGIN = 64;
	    
    
    private OperationKey key = null;
    
    protected String name;
    
    private String bgColorCode, fgColorCode,size, width, height, logo, text, outputFile;
       
    private QRCodeWriter qrCodeWriter = new QRCodeWriter();
    
    @Override
    public void init(Node node) throws InitializationException  {
        LOGGER.debug("Init start");
        try {            
            name =  XMLConfig.get(node, "@name");  	
        	
            bgColorCode = XMLConfig.get(node, "@bgColorCode");
            fgColorCode = XMLConfig.get(node, "@fgColorCode");
            
            
            size = XMLConfig.get(node, "@size");
            width = XMLConfig.get(node, "@width");
            height = XMLConfig.get(node, "@height");
            
            logo = XMLConfig.get(node, "@logo");
            text = XMLConfig.get(node, "@text");
            outputFile = XMLConfig.get(node, "@outputFile");
            
        } catch (Exception exc) {
            throw new InitializationException("GV_INIT_SERVICE_ERROR", new String[][]{{"message", exc.getMessage()}},
                    exc);
        }

    }
           

    @Override
    public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {
       
        try {
        	
        	  Integer backgroundColor = Long.decode(PropertiesHandler.expand(Optional.ofNullable(bgColorCode).orElse("#FFFFFFFF"), gvBuffer)).intValue();      	  
        	  Integer foregroundColor = Long.decode(PropertiesHandler.expand(Optional.ofNullable(fgColorCode).orElse("#FF000000"), gvBuffer)).intValue();
   
        	  Integer w,h;
        	  
        	  if (Objects.isNull(size)) {
        		  w = Integer.valueOf(PropertiesHandler.expand(width, gvBuffer));
            	  h = Integer.valueOf(PropertiesHandler.expand(height, gvBuffer));
        		  
        	  } else {
        		  int s = Integer.valueOf(PropertiesHandler.expand(size, gvBuffer));        		 
        		  w = s;
        		  h = s;
        	  }
        	  
        	  
        	  String content = null;
        	  
        	  if (gvBuffer.getObject() instanceof String) {
        		  content = (String) gvBuffer.getObject();        		  
        	  } else if (gvBuffer.getObject() instanceof byte[]) {
        		  content = new String((byte[])gvBuffer.getObject(), Optional.ofNullable(gvBuffer.getProperty("OBJECT_ENCODING")).orElse("UTF-8"));
        	  } else {
        		  content = Optional.ofNullable(gvBuffer.getObject()).orElse("NULL").toString();
        	  }
        	  
        	  Map<EncodeHintType, Object> hints = new HashMap<>();
        	  hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        	  
        	  MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(foregroundColor, backgroundColor);
        	  
        	  BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, w, h, hints);
        	  
        	  BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig);
        	         	  
        	  if (logo!=null) {
        		  
        		  String logoURI = PropertiesHandler.expand(logo, gvBuffer); 
        		       		        		  
        		  BufferedImage logoImage = logoURI.startsWith("http") ? ImageIO.read(new URL(logoURI)) :
        			                                                     ImageIO.read(Files.newInputStream(Paths.get(logoURI), StandardOpenOption.READ));
        		          		  
        		  int deltaHeight = qrImage.getHeight() - logoImage.getHeight();
        	      int deltaWidth = qrImage.getWidth() - logoImage.getWidth();
         	      BufferedImage combined = new BufferedImage(qrImage.getHeight(), qrImage.getWidth(), BufferedImage.TYPE_INT_ARGB);
        	    	
        	      Graphics2D g = (Graphics2D) combined.getGraphics();
        	      g.drawImage(qrImage, 0, 0, null);       	     
        	      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        	      g.drawImage(logoImage, (int) Math.round(deltaWidth / 2), (int) Math.round(deltaHeight / 2), null);
        		  
        		  qrImage = combined;
        		  
        	  }
        	  
        	  if (text!=null) {       		
        		  
        		  String actualText = PropertiesHandler.expand(text, gvBuffer);
        		  int textSize = actualText.length() * TEXT_CHARACTER_WITH;
        		  
        		  BufferedImage qrCodeWithText = new BufferedImage( textSize>qrImage.getWidth() ? textSize+TEXT_HORIZONTAL_MARGIN : qrImage.getWidth(), qrImage.getHeight()+TEXT_VERTICAL_MARGIN, BufferedImage.TYPE_INT_ARGB);
        		      		    
        		  Graphics graphics = qrCodeWithText.getGraphics();
        		  graphics.setColor(Color.decode(backgroundColor.toString()));
        		  graphics.fillRect(0, 0, qrCodeWithText.getWidth(), qrCodeWithText.getHeight());
        		  graphics.setColor(Color.decode(foregroundColor.toString()));
        		  graphics.setFont(new Font(TEXT_FONT_NAME, Font.PLAIN, TEXT_FONT_SIZE));       		    
        		    
        		  graphics.drawString(actualText, (qrCodeWithText.getWidth()/2) - (textSize/2), 48 );
        		  graphics.drawImage(qrImage, (qrCodeWithText.getWidth()/2) - (qrImage.getWidth()/2), 72, null);
        		  
        		  qrImage = qrCodeWithText;        	  
        	  }
        	  
        	  
        	  if (outputFile!=null) {        		  
        		  
        		  String actualOutputFile = PropertiesHandler.expand(outputFile, gvBuffer);        		  
        		  ImageIO.write(qrImage, "png", new File(actualOutputFile));
        		  
        		  gvBuffer.setObject(actualOutputFile);
        		  
        	  } else {
        		  try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
        	        	ImageIO.write(qrImage, "png", byteArrayOutputStream);
        	        	
        	        	 gvBuffer.setObject(byteArrayOutputStream.toByteArray());
        	        }  
        		 
        	  }      	 
        		          
           
        } catch (Exception exc) {
            throw new CallException("GV_CALL_SERVICE_ERROR", new String[][]{{"service", gvBuffer.getService()},
                    {"system", gvBuffer.getSystem()}, {"tid", gvBuffer.getId().toString()},
                    {"message", exc.getMessage()}}, exc);
        }
        return gvBuffer;
    }
  
    @Override
    public void cleanUp(){
        // do nothing
    }
    
    @Override
    public void destroy(){
        // do nothing
    }

    @Override
    public String getServiceAlias(GVBuffer gvBuffer){
        return gvBuffer.getService();
    }

    @Override
    public void setKey(OperationKey key){
        this.key = key;
    }
    
    @Override
    public OperationKey getKey(){
        return key;
    }
}
