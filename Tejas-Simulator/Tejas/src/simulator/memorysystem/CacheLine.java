/*****************************************************************************
 * Tejas Simulator
 * ------------------------------------------------------------------------------------------------------------
 * 
 * Copyright [2010] [Indian Institute of Technology, Delhi]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------------------------------------------------
 * 
 * Contributors: Moksh Upadhyay
 *****************************************************************************/
package memorysystem;

import java.util.LinkedList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

public class CacheLine implements Cloneable {
    private long              tag;
    private long              timestamp;
    private long              address;
    private MESIF             state       = MESIF.INVALID;
    private Cache             fw          = null;
    private boolean           isDirectory = false;
    
    private LinkedList<Cache> sharers     = null;
    
    public CacheLine(boolean isDirectory) {
        this.setTag(-1);
        this.setState(MESIF.INVALID);
        this.setTimestamp(0);
        this.setAddress(-1);
        this.isDirectory = isDirectory;
        
        if (isDirectory == true) {
            sharers = new LinkedList<Cache>();
        }
    }
    
    private void checkIsDirectory() {
        if (isDirectory == false) {
            misc.Error.showErrorAndExit(
                    "This method is supposed to be used by a directory only !!");
        }
    }
    
    public Cache getOwner() {
        
        checkIsDirectory();
        
        if (this.fw != null) {
            return fw;
        }
        
        if (sharers.size() == 0) {
            return null;
        } else if (sharers.size() == 1) {
            return sharers.get(0);
        } else {
            misc.Error.showErrorAndExit(
                    "This directory entry has multiple owners : " + this);
            return null;
        }
    }
    
    public boolean isSharer(Cache c) {
        checkIsDirectory();
        return (this.sharers.indexOf(c) != -1);
    }
    
    public boolean isFw(Cache c) {
        checkIsDirectory();
        if (this.fw == null)
            return false;
        return (this.fw == c);
    }
    
    public void addSharer(Cache c) {
        checkIsDirectory();
        if (this.state == MESIF.INVALID) {
            misc.Error.showErrorAndExit("Unholy mess !!");
        }
        
        // You cannot add a new sharer for a modified entry.
        // For same entry, if you try to add an event, it was because the cache
        // sent multiple requests for
        // the same cache line which triggered the memResponse multiple times.
        // For the time being, just ignore this hack.
        if (this.state == MESIF.MODIFIED && this.sharers.size() > 0
                && this.sharers.get(0) != c) {
            misc.Error.showErrorAndExit(
                    "You cannot have multiple owners for a modified state !!\n"
                            + "currentOwner : "
                            + getOwner().containingMemSys.getCore()
                                    .getCore_number()
                            + "\nnewOwner : "
                            + c.containingMemSys.getCore().getCore_number()
                            + "\naddr : " + this.getAddress());
        }
        
        // You cannot add a new sharer for exclusive entry.
        // For same entry, if you try to add an event, it was because the cache
        // sent multiple requests for
        // the same cache line which triggered the memResponse multiple times.
        // For the time being, just ignore this hack.
        if (this.state == MESIF.EXCLUSIVE && this.sharers.size() > 0
                && this.sharers.get(0) != c) {
            misc.Error.showErrorAndExit(
                    "You cannot have multiple owners for exclusive state !!\n"
                            + "currentOwner : "
                            + getOwner().containingMemSys.getCore()
                                    .getCore_number()
                            + "\nnewOwner : "
                            + c.containingMemSys.getCore().getCore_number()
                            + "\naddr : " + this.getAddress());
        }
        
        // FORWARD
        if (this.isSharer(c)) {
            this.fw = c;
            removeSharer(c);
            return;
        }
        
        if (fw != null) {
            this.sharers.add(fw);
            fw = c;
        }
        // EXCLUSIVE or MODIFIED
        if (this.getOwner() == null) {
            this.sharers.add(c);
        }
    }
    
    public Cache getFw() {
        return this.fw;
    }
    
    public void clearAllSharers() {
        checkIsDirectory();
        this.sharers.clear();
        this.fw = null;
    }
    
    public void removeSharer(Cache c) {
        checkIsDirectory();
        if (this.isSharer(c) == false) {
            misc.Error.showErrorAndExit(
                    "Trying to remove a sharer which is not a sharer !!");
        }
        if (sharers!=null) this.sharers.remove(c);
    }
    
    public Object clone() {
        try {
            // call clone in Object.
            return super.clone();
        } catch (CloneNotSupportedException e) {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }
    
    public boolean hasTagMatch(long tag) {
        return (tag == this.getTag());
    }
    
    public long getTag() {
        return tag;
    }
    
    public void setTag(long tag) {
        this.tag = tag;
    }
    
    public boolean isValid() {
        return (state != MESIF.INVALID);
    }
    
    public double getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isModified() {
        if (state == MESIF.MODIFIED)
            return true;
        else
            return false;
    }
    
    public MESIF getState() {
        return state;
    }
    
    public void setState(MESIF state) {
        this.state = state;
    }
    
    public long getAddress() {
        return address;
    }
    
    public void setAddress(long address) {
        this.address = address;
    }
    
    public LinkedList<Cache> getSharers() {
        checkIsDirectory();
        LinkedList<Cache> tmp = sharers;
        if (this.fw != null)
            tmp.add(this.fw);
        return tmp;
    }
    
    public Cache getFirstSharer() {
        checkIsDirectory();
        if (fw != null) {
            return fw;
        }
        return sharers.get(0);
    }
    
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("addr = " + this.getAddress() + " : " + "state = "
                + this.getState());
        if (this.isDirectory) {
            s.append(" cores : ");
            
            for (Cache c : sharers) {
                s.append(c.containingMemSys.getCore().getCore_number() + " , ");
            }
        }
        return s.toString();
    }
}
