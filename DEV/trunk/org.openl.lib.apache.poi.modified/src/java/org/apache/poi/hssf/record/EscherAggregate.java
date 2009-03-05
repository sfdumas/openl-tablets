/* ====================================================================
   Copyright 2003-2004   Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record;

import org.apache.poi.ddf.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.model.AbstractShape;
import org.apache.poi.hssf.model.TextboxShape;
import org.apache.poi.hssf.model.DrawingManager;
import org.apache.poi.hssf.model.ConvertAnchor;

import java.util.*;

/**
 * This class is used to aggregate the MSODRAWING and OBJ record combinations.
 * This is necessary due to the bizare way in which these records are
 * serialized. What happens is that you get a combination of MSODRAWING -> OBJ ->
 * MSODRAWING -> OBJ records but the escher records are serialized _across_ the
 * MSODRAWING records.
 * <p>
 * It gets even worse when you start looking at TXO records.
 * <p>
 * So what we do with this class is aggregate lazily. That is we don't aggregate
 * the MSODRAWING -> OBJ records unless we need to modify them.
 * 
 * 
 * @author Glen Stampoultzis (glens at apache.org)
 */
public class EscherAggregate extends AbstractEscherHolderRecord {
    public static final short sid = 9876;

    public static final short ST_MIN = (short) 0;
    public static final short ST_NOT_PRIMATIVE = ST_MIN;
    public static final short ST_RECTANGLE = (short) 1;
    public static final short ST_ROUNDRECTANGLE = (short) 2;
    public static final short ST_ELLIPSE = (short) 3;
    public static final short ST_DIAMOND = (short) 4;
    public static final short ST_ISOCELESTRIANGLE = (short) 5;
    public static final short ST_RIGHTTRIANGLE = (short) 6;
    public static final short ST_PARALLELOGRAM = (short) 7;
    public static final short ST_TRAPEZOID = (short) 8;
    public static final short ST_HEXAGON = (short) 9;
    public static final short ST_OCTAGON = (short) 10;
    public static final short ST_PLUS = (short) 11;
    public static final short ST_STAR = (short) 12;
    public static final short ST_ARROW = (short) 13;
    public static final short ST_THICKARROW = (short) 14;
    public static final short ST_HOMEPLATE = (short) 15;
    public static final short ST_CUBE = (short) 16;
    public static final short ST_BALLOON = (short) 17;
    public static final short ST_SEAL = (short) 18;
    public static final short ST_ARC = (short) 19;
    public static final short ST_LINE = (short) 20;
    public static final short ST_PLAQUE = (short) 21;
    public static final short ST_CAN = (short) 22;
    public static final short ST_DONUT = (short) 23;
    public static final short ST_TEXTSIMPLE = (short) 24;
    public static final short ST_TEXTOCTAGON = (short) 25;
    public static final short ST_TEXTHEXAGON = (short) 26;
    public static final short ST_TEXTCURVE = (short) 27;
    public static final short ST_TEXTWAVE = (short) 28;
    public static final short ST_TEXTRING = (short) 29;
    public static final short ST_TEXTONCURVE = (short) 30;
    public static final short ST_TEXTONRING = (short) 31;
    public static final short ST_STRAIGHTCONNECTOR1 = (short) 32;
    public static final short ST_BENTCONNECTOR2 = (short) 33;
    public static final short ST_BENTCONNECTOR3 = (short) 34;
    public static final short ST_BENTCONNECTOR4 = (short) 35;
    public static final short ST_BENTCONNECTOR5 = (short) 36;
    public static final short ST_CURVEDCONNECTOR2 = (short) 37;
    public static final short ST_CURVEDCONNECTOR3 = (short) 38;
    public static final short ST_CURVEDCONNECTOR4 = (short) 39;
    public static final short ST_CURVEDCONNECTOR5 = (short) 40;
    public static final short ST_CALLOUT1 = (short) 41;
    public static final short ST_CALLOUT2 = (short) 42;
    public static final short ST_CALLOUT3 = (short) 43;
    public static final short ST_ACCENTCALLOUT1 = (short) 44;
    public static final short ST_ACCENTCALLOUT2 = (short) 45;
    public static final short ST_ACCENTCALLOUT3 = (short) 46;
    public static final short ST_BORDERCALLOUT1 = (short) 47;
    public static final short ST_BORDERCALLOUT2 = (short) 48;
    public static final short ST_BORDERCALLOUT3 = (short) 49;
    public static final short ST_ACCENTBORDERCALLOUT1 = (short) 50;
    public static final short ST_ACCENTBORDERCALLOUT2 = (short) 51;
    public static final short ST_ACCENTBORDERCALLOUT3 = (short) 52;
    public static final short ST_RIBBON = (short) 53;
    public static final short ST_RIBBON2 = (short) 54;
    public static final short ST_CHEVRON = (short) 55;
    public static final short ST_PENTAGON = (short) 56;
    public static final short ST_NOSMOKING = (short) 57;
    public static final short ST_SEAL8 = (short) 58;
    public static final short ST_SEAL16 = (short) 59;
    public static final short ST_SEAL32 = (short) 60;
    public static final short ST_WEDGERECTCALLOUT = (short) 61;
    public static final short ST_WEDGERRECTCALLOUT = (short) 62;
    public static final short ST_WEDGEELLIPSECALLOUT = (short) 63;
    public static final short ST_WAVE = (short) 64;
    public static final short ST_FOLDEDCORNER = (short) 65;
    public static final short ST_LEFTARROW = (short) 66;
    public static final short ST_DOWNARROW = (short) 67;
    public static final short ST_UPARROW = (short) 68;
    public static final short ST_LEFTRIGHTARROW = (short) 69;
    public static final short ST_UPDOWNARROW = (short) 70;
    public static final short ST_IRREGULARSEAL1 = (short) 71;
    public static final short ST_IRREGULARSEAL2 = (short) 72;
    public static final short ST_LIGHTNINGBOLT = (short) 73;
    public static final short ST_HEART = (short) 74;
    public static final short ST_PICTUREFRAME = (short) 75;
    public static final short ST_QUADARROW = (short) 76;
    public static final short ST_LEFTARROWCALLOUT = (short) 77;
    public static final short ST_RIGHTARROWCALLOUT = (short) 78;
    public static final short ST_UPARROWCALLOUT = (short) 79;
    public static final short ST_DOWNARROWCALLOUT = (short) 80;
    public static final short ST_LEFTRIGHTARROWCALLOUT = (short) 81;
    public static final short ST_UPDOWNARROWCALLOUT = (short) 82;
    public static final short ST_QUADARROWCALLOUT = (short) 83;
    public static final short ST_BEVEL = (short) 84;
    public static final short ST_LEFTBRACKET = (short) 85;
    public static final short ST_RIGHTBRACKET = (short) 86;
    public static final short ST_LEFTBRACE = (short) 87;
    public static final short ST_RIGHTBRACE = (short) 88;
    public static final short ST_LEFTUPARROW = (short) 89;
    public static final short ST_BENTUPARROW = (short) 90;
    public static final short ST_BENTARROW = (short) 91;
    public static final short ST_SEAL24 = (short) 92;
    public static final short ST_STRIPEDRIGHTARROW = (short) 93;
    public static final short ST_NOTCHEDRIGHTARROW = (short) 94;
    public static final short ST_BLOCKARC = (short) 95;
    public static final short ST_SMILEYFACE = (short) 96;
    public static final short ST_VERTICALSCROLL = (short) 97;
    public static final short ST_HORIZONTALSCROLL = (short) 98;
    public static final short ST_CIRCULARARROW = (short) 99;
    public static final short ST_NOTCHEDCIRCULARARROW = (short) 100;
    public static final short ST_UTURNARROW = (short) 101;
    public static final short ST_CURVEDRIGHTARROW = (short) 102;
    public static final short ST_CURVEDLEFTARROW = (short) 103;
    public static final short ST_CURVEDUPARROW = (short) 104;
    public static final short ST_CURVEDDOWNARROW = (short) 105;
    public static final short ST_CLOUDCALLOUT = (short) 106;
    public static final short ST_ELLIPSERIBBON = (short) 107;
    public static final short ST_ELLIPSERIBBON2 = (short) 108;
    public static final short ST_FLOWCHARTPROCESS = (short) 109;
    public static final short ST_FLOWCHARTDECISION = (short) 110;
    public static final short ST_FLOWCHARTINPUTOUTPUT = (short) 111;
    public static final short ST_FLOWCHARTPREDEFINEDPROCESS = (short) 112;
    public static final short ST_FLOWCHARTINTERNALSTORAGE = (short) 113;
    public static final short ST_FLOWCHARTDOCUMENT = (short) 114;
    public static final short ST_FLOWCHARTMULTIDOCUMENT = (short) 115;
    public static final short ST_FLOWCHARTTERMINATOR = (short) 116;
    public static final short ST_FLOWCHARTPREPARATION = (short) 117;
    public static final short ST_FLOWCHARTMANUALINPUT = (short) 118;
    public static final short ST_FLOWCHARTMANUALOPERATION = (short) 119;
    public static final short ST_FLOWCHARTCONNECTOR = (short) 120;
    public static final short ST_FLOWCHARTPUNCHEDCARD = (short) 121;
    public static final short ST_FLOWCHARTPUNCHEDTAPE = (short) 122;
    public static final short ST_FLOWCHARTSUMMINGJUNCTION = (short) 123;
    public static final short ST_FLOWCHARTOR = (short) 124;
    public static final short ST_FLOWCHARTCOLLATE = (short) 125;
    public static final short ST_FLOWCHARTSORT = (short) 126;
    public static final short ST_FLOWCHARTEXTRACT = (short) 127;
    public static final short ST_FLOWCHARTMERGE = (short) 128;
    public static final short ST_FLOWCHARTOFFLINESTORAGE = (short) 129;
    public static final short ST_FLOWCHARTONLINESTORAGE = (short) 130;
    public static final short ST_FLOWCHARTMAGNETICTAPE = (short) 131;
    public static final short ST_FLOWCHARTMAGNETICDISK = (short) 132;
    public static final short ST_FLOWCHARTMAGNETICDRUM = (short) 133;
    public static final short ST_FLOWCHARTDISPLAY = (short) 134;
    public static final short ST_FLOWCHARTDELAY = (short) 135;
    public static final short ST_TEXTPLAINTEXT = (short) 136;
    public static final short ST_TEXTSTOP = (short) 137;
    public static final short ST_TEXTTRIANGLE = (short) 138;
    public static final short ST_TEXTTRIANGLEINVERTED = (short) 139;
    public static final short ST_TEXTCHEVRON = (short) 140;
    public static final short ST_TEXTCHEVRONINVERTED = (short) 141;
    public static final short ST_TEXTRINGINSIDE = (short) 142;
    public static final short ST_TEXTRINGOUTSIDE = (short) 143;
    public static final short ST_TEXTARCHUPCURVE = (short) 144;
    public static final short ST_TEXTARCHDOWNCURVE = (short) 145;
    public static final short ST_TEXTCIRCLECURVE = (short) 146;
    public static final short ST_TEXTBUTTONCURVE = (short) 147;
    public static final short ST_TEXTARCHUPPOUR = (short) 148;
    public static final short ST_TEXTARCHDOWNPOUR = (short) 149;
    public static final short ST_TEXTCIRCLEPOUR = (short) 150;
    public static final short ST_TEXTBUTTONPOUR = (short) 151;
    public static final short ST_TEXTCURVEUP = (short) 152;
    public static final short ST_TEXTCURVEDOWN = (short) 153;
    public static final short ST_TEXTCASCADEUP = (short) 154;
    public static final short ST_TEXTCASCADEDOWN = (short) 155;
    public static final short ST_TEXTWAVE1 = (short) 156;
    public static final short ST_TEXTWAVE2 = (short) 157;
    public static final short ST_TEXTWAVE3 = (short) 158;
    public static final short ST_TEXTWAVE4 = (short) 159;
    public static final short ST_TEXTINFLATE = (short) 160;
    public static final short ST_TEXTDEFLATE = (short) 161;
    public static final short ST_TEXTINFLATEBOTTOM = (short) 162;
    public static final short ST_TEXTDEFLATEBOTTOM = (short) 163;
    public static final short ST_TEXTINFLATETOP = (short) 164;
    public static final short ST_TEXTDEFLATETOP = (short) 165;
    public static final short ST_TEXTDEFLATEINFLATE = (short) 166;
    public static final short ST_TEXTDEFLATEINFLATEDEFLATE = (short) 167;
    public static final short ST_TEXTFADERIGHT = (short) 168;
    public static final short ST_TEXTFADELEFT = (short) 169;
    public static final short ST_TEXTFADEUP = (short) 170;
    public static final short ST_TEXTFADEDOWN = (short) 171;
    public static final short ST_TEXTSLANTUP = (short) 172;
    public static final short ST_TEXTSLANTDOWN = (short) 173;
    public static final short ST_TEXTCANUP = (short) 174;
    public static final short ST_TEXTCANDOWN = (short) 175;
    public static final short ST_FLOWCHARTALTERNATEPROCESS = (short) 176;
    public static final short ST_FLOWCHARTOFFPAGECONNECTOR = (short) 177;
    public static final short ST_CALLOUT90 = (short) 178;
    public static final short ST_ACCENTCALLOUT90 = (short) 179;
    public static final short ST_BORDERCALLOUT90 = (short) 180;
    public static final short ST_ACCENTBORDERCALLOUT90 = (short) 181;
    public static final short ST_LEFTRIGHTUPARROW = (short) 182;
    public static final short ST_SUN = (short) 183;
    public static final short ST_MOON = (short) 184;
    public static final short ST_BRACKETPAIR = (short) 185;
    public static final short ST_BRACEPAIR = (short) 186;
    public static final short ST_SEAL4 = (short) 187;
    public static final short ST_DOUBLEWAVE = (short) 188;
    public static final short ST_ACTIONBUTTONBLANK = (short) 189;
    public static final short ST_ACTIONBUTTONHOME = (short) 190;
    public static final short ST_ACTIONBUTTONHELP = (short) 191;
    public static final short ST_ACTIONBUTTONINFORMATION = (short) 192;
    public static final short ST_ACTIONBUTTONFORWARDNEXT = (short) 193;
    public static final short ST_ACTIONBUTTONBACKPREVIOUS = (short) 194;
    public static final short ST_ACTIONBUTTONEND = (short) 195;
    public static final short ST_ACTIONBUTTONBEGINNING = (short) 196;
    public static final short ST_ACTIONBUTTONRETURN = (short) 197;
    public static final short ST_ACTIONBUTTONDOCUMENT = (short) 198;
    public static final short ST_ACTIONBUTTONSOUND = (short) 199;
    public static final short ST_ACTIONBUTTONMOVIE = (short) 200;
    public static final short ST_HOSTCONTROL = (short) 201;
    public static final short ST_TEXTBOX = (short) 202;
    public static final short ST_NIL = (short) 0x0FFF;

    protected HSSFPatriarch patriarch;

    /** Maps shape container objects to their OBJ records */
    private Map shapeToObj = new HashMap();
    private DrawingManager drawingManager;
    private short drawingGroupId;

    public EscherAggregate(DrawingManager drawingManager) {
        this.drawingManager = drawingManager;
    }

    /**
     * @return Returns the current sid.
     */
    public short getSid() {
        return sid;
    }

    /**
     * Unused since this is an aggregate record. Use createAggregate().
     * 
     * @see #createAggregate
     */
    protected void fillFields(byte[] data, short size, int offset) {
        throw new IllegalStateException("Should not reach here");
    }

    /**
     * Calculates the string representation of this record. This is simply a
     * dump of all the records.
     */
    public String toString() {
        String nl = System.getProperty("line.separtor");

        StringBuffer result = new StringBuffer();
        result.append('[').append(getRecordName()).append(']' + nl);
        for (Iterator iterator = getEscherRecords().iterator(); iterator.hasNext();) {
            EscherRecord escherRecord = (EscherRecord) iterator.next();
            result.append(escherRecord.toString());
        }
        result.append("[/").append(getRecordName()).append(']' + nl);

        return result.toString();
    }

    /**
     * Collapses the drawing records into an aggregate.
     */
    public static EscherAggregate createAggregate(List records, int locFirstDrawingRecord, DrawingManager drawingManager) {
        // Keep track of any shape records created so we can match them back to
        // the object id's.
        // Textbox objects are also treated as shape objects.
        final List shapeRecords = new ArrayList();
        EscherRecordFactory recordFactory = new DefaultEscherRecordFactory() {
            public EscherRecord createRecord(byte[] data, int offset) {
                EscherRecord r = super.createRecord(data, offset);
                if (r.getRecordId() == EscherClientDataRecord.RECORD_ID
                        || r.getRecordId() == EscherTextboxRecord.RECORD_ID) {
                    shapeRecords.add(r);
                }
                return r;
            }
        };

        // Calculate the size of the buffer
        EscherAggregate agg = new EscherAggregate(drawingManager);
        int loc = locFirstDrawingRecord;
        int dataSize = 0;
        while (loc + 1 < records.size() && sid(records, loc) == DrawingRecord.sid && isObjectRecord(records, loc + 1)) {
            dataSize += ((DrawingRecord) records.get(loc)).getData().length;
            loc += 2;
        }

        // Create one big buffer
        byte buffer[] = new byte[dataSize];
        int offset = 0;
        loc = locFirstDrawingRecord;
        while (loc + 1 < records.size() && sid(records, loc) == DrawingRecord.sid && isObjectRecord(records, loc + 1)) {
            DrawingRecord drawingRecord = (DrawingRecord) records.get(loc);
            System.arraycopy(drawingRecord.getData(), 0, buffer, offset, drawingRecord.getData().length);
            offset += drawingRecord.getData().length;
            loc += 2;
        }

        // Decode the shapes
        // agg.escherRecords = new ArrayList();
        int pos = 0;
        while (pos < dataSize) {
            EscherRecord r = recordFactory.createRecord(buffer, pos);
            int bytesRead = r.fillFields(buffer, pos, recordFactory);
            agg.addEscherRecord(r);
            pos += bytesRead;
        }

        // Associate the object records with the shapes
        loc = locFirstDrawingRecord;
        int shapeIndex = 0;
        agg.shapeToObj = new HashMap();
        while (loc + 1 < records.size() && sid(records, loc) == DrawingRecord.sid && isObjectRecord(records, loc + 1)) {
            Record objRecord = (Record) records.get(loc + 1);
            agg.shapeToObj.put(shapeRecords.get(shapeIndex++), objRecord);
            loc += 2;
        }

        return agg;

    }

    /**
     * Serializes this aggregate to a byte array. Since this is an aggregate
     * record it will effectively serialize the aggregated records.
     * 
     * @param offset The offset into the start of the array.
     * @param data The byte array to serialize to.
     * @return The number of bytes serialized.
     */
    public int serialize(int offset, byte[] data) {
        convertUserModelToRecords();

        // Determine buffer size
        List records = getEscherRecords();
        int size = getEscherRecordSize(records);
        byte[] buffer = new byte[size];

        // Serialize escher records into one big data structure and keep note of
        // ending offsets.
        final List spEndingOffsets = new ArrayList();
        final List shapes = new ArrayList();
        int pos = 0;
        for (Iterator iterator = records.iterator(); iterator.hasNext();) {
            EscherRecord e = (EscherRecord) iterator.next();
            pos += e.serialize(pos, buffer, new EscherSerializationListener() {
                public void beforeRecordSerialize(int offset, short recordId, EscherRecord record) {
                }

                public void afterRecordSerialize(int offset, short recordId, int size, EscherRecord record) {
                    if (recordId == EscherClientDataRecord.RECORD_ID || recordId == EscherTextboxRecord.RECORD_ID) {
                        spEndingOffsets.add(new Integer(offset));
                        shapes.add(record);
                    }
                }
            });
        }
        // todo: fix this
        shapes.add(0, null);
        spEndingOffsets.add(0, null);

        // Split escher records into separate MSODRAWING and OBJ, TXO records.
        // (We don't break on
        // the first one because it's the patriach).
        pos = offset;
        for (int i = 1; i < shapes.size(); i++) {
            int endOffset = ((Integer) spEndingOffsets.get(i)).intValue() - 1;
            int startOffset;
            if (i == 1)
                startOffset = 0;
            else
                startOffset = ((Integer) spEndingOffsets.get(i - 1)).intValue();

            // Create and write a new MSODRAWING record
            DrawingRecord drawing = new DrawingRecord();
            byte[] drawingData = new byte[endOffset - startOffset + 1];
            System.arraycopy(buffer, startOffset, drawingData, 0, drawingData.length);
            drawing.setData(drawingData);
            int temp = drawing.serialize(pos, data);
            pos += temp;

            // Write the matching OBJ record
            Record obj = (Record) shapeToObj.get(shapes.get(i));
            temp = obj.serialize(pos, data);
            pos += temp;

        }

        int bytesWritten = pos - offset;
        if (bytesWritten != getRecordSize())
            throw new RecordFormatException(bytesWritten + " bytes written but getRecordSize() reports "
                    + getRecordSize());
        return bytesWritten;
    }

    /**
     * How many bytes do the raw escher records contain.
     * 
     * @param records List of escher records
     * @return the number of bytes
     */
    private int getEscherRecordSize(List records) {
        int size = 0;
        for (Iterator iterator = records.iterator(); iterator.hasNext();)
            size += ((EscherRecord) iterator.next()).getRecordSize();
        return size;
    }

    /**
     * The number of bytes required to serialize this record.
     */
    public int getRecordSize() {
        convertUserModelToRecords();
        List records = getEscherRecords();
        int rawEscherSize = getEscherRecordSize(records);
        int drawingRecordSize = rawEscherSize + (shapeToObj.size()) * 4;
        int objRecordSize = 0;
        for (Iterator iterator = shapeToObj.values().iterator(); iterator.hasNext();) {
            Record r = (Record) iterator.next();
            objRecordSize += r.getRecordSize();
        }
        return drawingRecordSize + objRecordSize;
    }

    /**
     * Associates an escher record to an OBJ record or a TXO record.
     */
    public Object assoicateShapeToObjRecord(EscherRecord r, Record objRecord) {
        return shapeToObj.put(r, objRecord);
    }

    public HSSFPatriarch getPatriarch() {
        return patriarch;
    }

    public void setPatriarch(HSSFPatriarch patriarch) {
        this.patriarch = patriarch;
    }

    public void clear() {
        clearEscherRecords();
        shapeToObj.clear();
        // lastShapeId = 1024;
    }

    protected String getRecordName() {
        return "ESCHERAGGREGATE";
    }

    // =============== Private methods ========================

    private static boolean isObjectRecord(List records, int loc) {
        return sid(records, loc) == ObjRecord.sid || sid(records, loc) == TextObjectRecord.sid;
    }

    private void convertUserModelToRecords() {
        if (patriarch != null) {
            shapeToObj.clear();
            clearEscherRecords();
            if (patriarch.getChildren().size() != 0) {
                convertPatriarch(patriarch);
                EscherContainerRecord dgContainer = (EscherContainerRecord) getEscherRecord(0);
                EscherContainerRecord spgrContainer = null;
                for (int i = 0; i < dgContainer.getChildRecords().size(); i++)
                    if (dgContainer.getChild(i).getRecordId() == EscherContainerRecord.SPGR_CONTAINER)
                        spgrContainer = (EscherContainerRecord) dgContainer.getChild(i);
                convertShapes(patriarch, spgrContainer, shapeToObj);

                patriarch = null;
            }
        }
    }

    private void convertShapes(HSSFShapeContainer parent, EscherContainerRecord escherParent, Map shapeToObj) {
        if (escherParent == null)
            throw new IllegalArgumentException("Parent record required");

        List shapes = parent.getChildren();
        for (Iterator iterator = shapes.iterator(); iterator.hasNext();) {
            HSSFShape shape = (HSSFShape) iterator.next();
            if (shape instanceof HSSFShapeGroup) {
                convertGroup((HSSFShapeGroup) shape, escherParent, shapeToObj);
            } else {
                AbstractShape shapeModel = AbstractShape.createShape(shape, drawingManager
                        .allocateShapeId(drawingGroupId));
                shapeToObj.put(findClientData(shapeModel.getSpContainer()), shapeModel.getObjRecord());
                if (shapeModel instanceof TextboxShape) {
                    EscherRecord escherTextbox = ((TextboxShape) shapeModel).getEscherTextbox();
                    shapeToObj.put(escherTextbox, ((TextboxShape) shapeModel).getTextObjectRecord());
                    // escherParent.addChildRecord(escherTextbox);
                }
                escherParent.addChildRecord(shapeModel.getSpContainer());
            }
        }
    }

    private void convertGroup(HSSFShapeGroup shape, EscherContainerRecord escherParent, Map shapeToObj) {
        EscherContainerRecord spgrContainer = new EscherContainerRecord();
        EscherContainerRecord spContainer = new EscherContainerRecord();
        EscherSpgrRecord spgr = new EscherSpgrRecord();
        EscherSpRecord sp = new EscherSpRecord();
        EscherOptRecord opt = new EscherOptRecord();
        EscherRecord anchor;
        EscherClientDataRecord clientData = new EscherClientDataRecord();

        spgrContainer.setRecordId(EscherContainerRecord.SPGR_CONTAINER);
        spgrContainer.setOptions((short) 0x000F);
        spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
        spContainer.setOptions((short) 0x000F);
        spgr.setRecordId(EscherSpgrRecord.RECORD_ID);
        spgr.setOptions((short) 0x0001);
        spgr.setRectX1(shape.getX1());
        spgr.setRectY1(shape.getY1());
        spgr.setRectX2(shape.getX2());
        spgr.setRectY2(shape.getY2());
        sp.setRecordId(EscherSpRecord.RECORD_ID);
        sp.setOptions((short) 0x0002);
        int shapeId = drawingManager.allocateShapeId(drawingGroupId);
        sp.setShapeId(shapeId);
        if (shape.getAnchor() instanceof HSSFClientAnchor)
            sp.setFlags(EscherSpRecord.FLAG_GROUP | EscherSpRecord.FLAG_HAVEANCHOR);
        else
            sp.setFlags(EscherSpRecord.FLAG_GROUP | EscherSpRecord.FLAG_HAVEANCHOR | EscherSpRecord.FLAG_CHILD);
        opt.setRecordId(EscherOptRecord.RECORD_ID);
        opt.setOptions((short) 0x0023);
        opt.addEscherProperty(new EscherBoolProperty(EscherProperties.PROTECTION__LOCKAGAINSTGROUPING, 0x00040004));
        opt.addEscherProperty(new EscherBoolProperty(EscherProperties.GROUPSHAPE__PRINT, 0x00080000));

        anchor = ConvertAnchor.createAnchor(shape.getAnchor());
        // clientAnchor.setCol1( ( (HSSFClientAnchor) shape.getAnchor()
        // ).getCol1() );
        // clientAnchor.setRow1( (short) ( (HSSFClientAnchor) shape.getAnchor()
        // ).getRow1() );
        // clientAnchor.setDx1( (short) shape.getAnchor().getDx1() );
        // clientAnchor.setDy1( (short) shape.getAnchor().getDy1() );
        // clientAnchor.setCol2( ( (HSSFClientAnchor) shape.getAnchor()
        // ).getCol2() );
        // clientAnchor.setRow2( (short) ( (HSSFClientAnchor) shape.getAnchor()
        // ).getRow2() );
        // clientAnchor.setDx2( (short) shape.getAnchor().getDx2() );
        // clientAnchor.setDy2( (short) shape.getAnchor().getDy2() );
        clientData.setRecordId(EscherClientDataRecord.RECORD_ID);
        clientData.setOptions((short) 0x0000);

        spgrContainer.addChildRecord(spContainer);
        spContainer.addChildRecord(spgr);
        spContainer.addChildRecord(sp);
        spContainer.addChildRecord(opt);
        spContainer.addChildRecord(anchor);
        spContainer.addChildRecord(clientData);

        ObjRecord obj = new ObjRecord();
        CommonObjectDataSubRecord cmo = new CommonObjectDataSubRecord();
        cmo.setObjectType(CommonObjectDataSubRecord.OBJECT_TYPE_GROUP);
        cmo.setObjectId((short) (shapeId));
        cmo.setLocked(true);
        cmo.setPrintable(true);
        cmo.setAutofill(true);
        cmo.setAutoline(true);
        GroupMarkerSubRecord gmo = new GroupMarkerSubRecord();
        EndSubRecord end = new EndSubRecord();
        obj.addSubRecord(cmo);
        obj.addSubRecord(gmo);
        obj.addSubRecord(end);
        shapeToObj.put(clientData, obj);

        escherParent.addChildRecord(spgrContainer);

        convertShapes(shape, spgrContainer, shapeToObj);

    }

    private EscherRecord findClientData(EscherContainerRecord spContainer) {
        for (Iterator iterator = spContainer.getChildRecords().iterator(); iterator.hasNext();) {
            EscherRecord r = (EscherRecord) iterator.next();
            if (r.getRecordId() == EscherClientDataRecord.RECORD_ID)
                return r;
        }
        throw new IllegalArgumentException("Can not find client data record");
    }

    private void convertPatriarch(HSSFPatriarch patriarch) {
        EscherContainerRecord dgContainer = new EscherContainerRecord();
        EscherDgRecord dg;
        EscherContainerRecord spgrContainer = new EscherContainerRecord();
        EscherContainerRecord spContainer1 = new EscherContainerRecord();
        EscherSpgrRecord spgr = new EscherSpgrRecord();
        EscherSpRecord sp1 = new EscherSpRecord();

        dgContainer.setRecordId(EscherContainerRecord.DG_CONTAINER);
        dgContainer.setOptions((short) 0x000F);
        dg = drawingManager.createDgRecord();
        drawingGroupId = dg.getDrawingGroupId();
        // dg.setOptions( (short) ( drawingId << 4 ) );
        // dg.setNumShapes( getNumberOfShapes( patriarch ) );
        // dg.setLastMSOSPID( 0 ); // populated after all shape id's are
        // assigned.
        spgrContainer.setRecordId(EscherContainerRecord.SPGR_CONTAINER);
        spgrContainer.setOptions((short) 0x000F);
        spContainer1.setRecordId(EscherContainerRecord.SP_CONTAINER);
        spContainer1.setOptions((short) 0x000F);
        spgr.setRecordId(EscherSpgrRecord.RECORD_ID);
        spgr.setOptions((short) 0x0001); // don't know what the 1 is for.
        spgr.setRectX1(patriarch.getX1());
        spgr.setRectY1(patriarch.getY1());
        spgr.setRectX2(patriarch.getX2());
        spgr.setRectY2(patriarch.getY2());
        sp1.setRecordId(EscherSpRecord.RECORD_ID);
        sp1.setOptions((short) 0x0002);
        sp1.setShapeId(drawingManager.allocateShapeId(dg.getDrawingGroupId()));
        sp1.setFlags(EscherSpRecord.FLAG_GROUP | EscherSpRecord.FLAG_PATRIARCH);

        dgContainer.addChildRecord(dg);
        dgContainer.addChildRecord(spgrContainer);
        spgrContainer.addChildRecord(spContainer1);
        spContainer1.addChildRecord(spgr);
        spContainer1.addChildRecord(sp1);

        addEscherRecord(dgContainer);
    }

    /** Retrieve the number of shapes (including the patriarch). */
    // private int getNumberOfShapes( HSSFPatriarch patriarch )
    // {
    // return patriarch.countOfAllChildren();
    // }
    private static short sid(List records, int loc) {
        return ((Record) records.get(loc)).getSid();
    }

}
