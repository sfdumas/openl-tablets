/* ====================================================================
 Copyright 2002-2004   Apache Software Foundation

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

package org.apache.poi.hdf.event;

import org.apache.poi.hdf.model.hdftypes.ChpxNode;
import org.apache.poi.hdf.model.hdftypes.PapxNode;
import org.apache.poi.hdf.model.hdftypes.SepxNode;
import org.apache.poi.hdf.model.hdftypes.TextPiece;
import org.apache.poi.hdf.model.hdftypes.DocumentProperties;
import org.apache.poi.hdf.model.hdftypes.FontTable;
import org.apache.poi.hdf.model.hdftypes.ListTables;
import org.apache.poi.hdf.model.hdftypes.StyleSheet;

public interface HDFLowLevelParsingListener {
    public void mainDocument(byte[] mainDocument);

    public void tableStream(byte[] tableStream);

    public void document(DocumentProperties dop);

    public void bodySection(SepxNode sepx);

    public void paragraph(PapxNode papx);

    public void characterRun(ChpxNode chpx);

    public void hdrSection(SepxNode sepx);

    public void endSections();

    public void text(TextPiece t);

    public void fonts(FontTable fontTbl);

    public void lists(ListTables listTbl);

    public void styleSheet(StyleSheet stsh);

    public void miscellaneous(int fcMin, int ccpText, int ccpFtn, int fcPlcfhdd, int lcbPlcfhdd);
}
