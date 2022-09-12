package interactivegrid2;
import utils.*;
import java.io.*;
import java.util.Vector;
import jni.*;

// We define a Grid class here, representing an electrocorticography (ECoG) grid.
// These grids are used on the OR to detect the focus of epileptic activity.
// Grid objects have the following constructor:
// public Grid(int nx, int ny)
// To make a new 5x4 grid object, use:
// Grid G = new Grid(5,4);
// This is a grid with 5 electrodes in x direction and 4 in y direction
// For the grids in the dept KNF, numbering starts in right upper corner,
// then goes down to right lower corner, then shifts to the second last column on the grid
//Grid has the properties Electrode, grid dimensions nx and ny, and firstNumber

public class Grid {
    private render.World world;
    private Electrode electrodes[][];
    private Vector<Electrode> electrodeList=new Vector<Electrode>();
    private render.InteractiveObject electrodeGroup=null;
    private static Volim volume=null;
    private static Volim brainV=new Volim("brainV");
    private static Volim inverseV=new Volim("inverseV");
    private static Volim distanceV=new Volim("distanceV");
    
    //create volume objects to store distances to the brain in x, y, z direction
    private static Volim distXvolim = new Volim("distX");
    private static Volim distYvolim = new Volim("distY");
    private static Volim distZvolim = new Volim("distZ");
    private static Volim distXvolimInv = new Volim("distXi");
    private static Volim distYvolimInv = new Volim("distYi");
    private static Volim distZvolimInv = new Volim("distZi");
    
    protected int nx=0, ny=0;
    protected int firstNumber=1;
    protected boolean projectToBrain=true;
    protected double deltaStep=1;
    protected double weightDistUser=1, weightDistMutual=1, weightDistBend=10,weightDistCortex=1;
    protected int divisionCortex=0;
    public void setFirstNumber(int x) {
        if (x>=0) {
            firstNumber=x;
            for(int i=0;i<electrodeList.size();i++) {
            electrodeList.get(i).setName("el"+(firstNumber+i));
            }
        }
    }
    public int getFirstNumber() {
        return firstNumber;
    }
    
    public Grid(render.World w) {
        world=w;
    }
    public Grid(render.World w, int nx, int ny) {
        world=w;
        setDim(nx, ny);
    }
    
    public void clear() {
        setDim(0,0);
    }
    
    public void setDim(int nx, int ny) {
        render.InteractiveObject.highestLevel(world, true);
        // Remove group and objects from world
        if (electrodeGroup!=null) {
            render.InteractiveObject.deselectAll(world);
            electrodeGroup.setSelected(world, true);
            render.InteractiveObject.ungroup(world);
            electrodeGroup=null;
        }
        for(Electrode e : electrodeList) {
            e.getUserObject().dispose();
            e.getPredictedObject().dispose();
        }
        this.nx=nx;
        this.ny=ny;
        electrodeList.clear();
        if (nx==0 || ny==0) {
            electrodes=null;
            return;
        }
        electrodes = new Electrode[nx][ny];
        for(int y=0;y<ny;y++) {
            for(int x=0;x<nx;x++) {
                Electrode e=new Electrode(world, x, y); // In adding an object, deselectAll is called.
                electrodes[x][y]=e;
                electrodeList.add(e);
                e.setName("el"+(firstNumber+(x+y*nx)));
            }
        }
        for(Electrode e : electrodeList) {
            e.getUserObject().setSelected(world, true);
            e.getPredictedObject().setSelected(world, true);
        }
        //render.InteractiveObject.print(world);
        electrodeGroup=render.InteractiveObject.group(world);
        setName("Grid "+nx+"x"+ny);
        for(Electrode e : electrodeList) {
            e.getPredictedObject().setSelectable(false);
        }
    }
    public void setName(String s) {
        if (electrodeGroup!=null) electrodeGroup.setName(s);
    }
    public String getName() {
        if (electrodeGroup!=null) return electrodeGroup.getName();
        return "";
    }
    public int getNX() {
        return nx;
    }
    public int getNY() {
        return ny;
    }
    public int getNXY() {
        return nx*ny;
    }
    public render.InteractiveObject getGroupObject() {
        return electrodeGroup;
    }
    public Electrode getElectrode(int x, int y) {
        if (x>=0 && x<nx && y>=0 && y<ny) {
            return electrodes[x][y];
        }
        else return null;
    }
    public Vector<Electrode> getElectrodeList() {
        return electrodeList;
    }
    public int firstSelectedObject() {
        for(int i=0;i<electrodeList.size();i++) {
            Electrode e=electrodeList.get(i);
            if (e.getUserObject().getSelected()) return i;
            if (e.getPredictedObject().getVisible() && e.getPredictedObject().getSelected()) return i;
        }
        return -1;
    }
    
    // check which electrodes have been positioned by user
    public Vector<Integer> electrodesDefined(){
        Vector<Integer> definedElectrodes=new Vector<>();
        for(int i=0; i<electrodeList.size(); i++) {
            if (electrodeList.get(i).getUserPos()!=null) definedElectrodes.add(i);
        }
        return definedElectrodes;
    }
    public String toString() {
        String s="";
        for(int i=0;i<electrodeList.size();i++) {
            Electrode e=electrodeList.get(i);
            s+=i+firstNumber+"\t";
            s+=e.getUserPos()+"\t";
            s+=e.getPredictedPos()+"\t";
            s+=e.getUserObject()+"\t";
            s+=e.getPredictedObject()+"\n";
        }
        return s;
    }
    public boolean read(String filename) {
        if (filename==null) return false;
        try {
            FileReader f=new FileReader(filename);
            LineNumberReader l=new LineNumberReader(f);
            String st;
            st=utils.searchVal(l, "dimensions");
            if (st!=null) {
                double dim[]=utils.ReadVec(st);
                if (dim.length!=2) {
                    System.err.println("Illegal number of dimensions");
                    return false;
                }
                else setDim((int) Math.round(dim[0]), (int) Math.round(dim[1]));
            }
            st=utils.searchVal(l, "firstNumber");
            if (st!=null) firstNumber=Integer.parseInt(st);
                for(int i=0; i<electrodeList.size(); i++) {
                    Electrode e=electrodeList.get(i);
                    st=utils.searchVal(l, "userVec", i);
                    if (st!=null && !st.equals("null")) {
                        e.setUserPos(new Vec(utils.ReadVec(st)));
                    }
                    else e.setUserPos(null);
                    st=utils.searchVal(l, "predictedVec", i);
                    if (st!=null && !st.equals("null")) {
                        e.setPredictedPos(new Vec(utils.ReadVec(st)));
                    }
                    else e.setPredictedPos(null);
                }
            f.close();
        }
        catch ( IOException ioe ) {
            System.err.println("File "+filename + " could not be opened for reading.");
            return false;
        }
        catch (NumberFormatException nfe) {
            System.out.println("Illegal numbers in "+filename+".");
            System.err.println("Illegal numbers in "+filename+".");
            return false;
        }
        return true;
    }
    public void save(String filename) {
        if (filename==null) return;
        try {System.out.println("Saving data to "+filename);
            FileWriter f=new FileWriter(filename);
            PrintWriter l=new PrintWriter(f);
            l.println("dimensions="+nx+" "+ny);
            l.println("firstNumber="+firstNumber);
            for(Electrode e : electrodeList) {
                if (e.getUserPos()==null) l.println("userVec=null");
                else l.println("userVec="+utils.WriteVec(e.getUserPos().data));
                
                if (e.getPredictedPos()==null) l.println("predictedVec=null");
                else l.println("predictedVec="+utils.WriteVec(e.getPredictedPos().data));
            }
            f.close();
        }
        catch (IOException ioe) {
        System.err.println("IO exception when writing "+filename);
        }
    }
    public void setVolume(Volim v) {
        volume=v;
        brainV=null;
    }
    public void FitGridAndProject() {
        // Works somewhat but FitGridByPointMatching followed by
        FineTuneByEnergyOptimization works better
        System.out.println("FitGridAndProject");
        try {                           
            int[] nrDefined = new int[3];
            int count = 0;
            //copy electrodeUser vectors to electrodePredicted vectors
            int i=0;
            for(Electrode e : electrodeList) {
                Vec v=e.getUserPos();
                if (v!=null && count<3) nrDefined[count++]=i; // An user-editable object has been defined for this electrode
                e.setPredictedPos(v);
                i++;
            }
            System.out.println("count is " + count);
            //create a volume object with MRI as well as inverted MRI data "brainV" and "brainVInverted"
            brainV=new Volim("brainV");
            brainV.set(volume);
            // brainV.read("brain.ics");
            //make hard brain edges in case edges are soft
            if (brainV.getMax()>1) brainV.threshold(128);
            brainV.closing(10); // Smooth surface
            inverseV.set(brainV); //set same dimensions
            inverseV.set(1);
            inverseV.sub(brainV);
            
            //brainV.write("c:\\temp\\brainV.ics");
            //inverseV.write("c:\\temp\\brainInverseV.ics");
            
            //conversion from voxel to 3D (elektrode) space coordinates
            int dim[]=brainV.getDimensions();
            double voxel[]=brainV.getVoxelDimensions();
            System.out.println(utils.WriteVec(voxel));
            //fetch for each point in this volume the 3D coordinates of distance to the brain
            //electrode outside brain
            distXvolim.vectorDistance(brainV, distYvolim, distZvolim);
            //electrodes inside brain
            distXvolimInv.vectorDistance(inverseV,distYvolimInv, distZvolimInv);
            
            // Volumes can be added      
            distXvolim.add(distXvolimInv);
            distYvolim.add(distYvolimInv);
            distZvolim.add(distZvolimInv);

            
            System.out.println(distXvolim);
            System.out.println(distXvolimInv);System.out.println(distYvolim);
            System.out.println(distYvolimInv);
            System.out.println(distZvolim);
            System.out.println(distZvolimInv);
            
            Vec vShiftVec = new Vec();
            Vec vShiftInvVec = new Vec();
            // use first three defined electrodes for extrapolation of 2d grids
            if (count>=3 && (ny>1 && nx>1)){
                //fetch grid indices of first three defined electrodes
                int[] g1tmp = electrodeList.get(nrDefined[0]).xy;
                int[] g2tmp = electrodeList.get(nrDefined[1]).xy;
                int[] g3tmp = electrodeList.get(nrDefined[2]).xy;
                //store 2D grid positions in 3D vectors g1, g2, and g3
                // using homogeneous coordinates, allowing us to capture rotation
                // and translation via one transformation matrix
                Vec g1 = new Vec(g1tmp[0], g1tmp[1],1);
                Vec g2 = new Vec(g2tmp[0], g2tmp[1],1);
                Vec g3 = new Vec(g3tmp[0], g3tmp[1],1);
                //store electrode positions in vectors v1, v2, and v3
                Vec v1 = electrodes[g1tmp[0]][g1tmp[1]].getPredictedPos();
                Vec v2 = electrodes[g2tmp[0]][g2tmp[1]].getPredictedPos();
                Vec v3 = electrodes[g3tmp[0]][g3tmp[1]].getPredictedPos();
                //set g1, g2, g3 as columns in matrix G
                Mat G = new Mat(3,3);
                G.setCol(0, g1);
                G.setCol(1, g2);
                G.setCol(2, g3);
                //set v1, v2, v3 as columns in matrix V
                Mat V = new Mat(3,3);
                V.setCol(0, v1);
                V.setCol(1, v2);
                V.setCol(2, v3);
                //calculate U in U*G = V using U*G*G^{-1} = V*G^{-1}
                Mat Ginv = new Mat(3,3);
                Ginv.inverse(G);
                Mat U = new Mat(3,3);
                U.dot(V,Ginv);
                //retrieve the other electrode positions using v = U*g
                Vec gVec = new Vec();
                Vec vVec = new Vec();
                int[] gtmp;
                gVec.setZ(1);
                int iElectrode=firstNumber;
                for(Electrode e : electrodeList) {
                    System.out.println("Electrode "+iElectrode+", grid pos=("+e.xy[0] + "," + e.xy[1]+")");
                    iElectrode++;
                    gVec.setX(e.xy[0]);
                    gVec.setY(e.xy[1]);
                    vVec.dot(U,gVec);
                    System.out.println("vVec before projecting "+vVec);
                    //bring electrodes, if not user-defined, to the brain
                    if (e.getUserPos()==null && projectToBrain){
                        //conversion from electrode space to voxel space
                        Vec vVoxel=new Vec();
                        for(int j=0;j<3;j++) vVoxel.setV(j,vVec.getV(j)/voxel[j]+dim[j]/2);
                        System.out.println("vVoxel="+vVoxel);
                        //getVal werkt in voxelspace/eenheden
                        vShiftVec.set(distXvolim.getVal(vVoxel,2),
                        distYvolim.getVal(vVoxel,2), distZvolim.getVal(vVoxel,2));
                        //vShiftInvVec.set(distXvolimInv.getVal(vVoxel,2), distYvolimInv.getVal(vVoxel,2), distZvolimInv.getVal(vVoxel,2));
                        //output of vectorDistance is in electrode space, no conversion needed
                        System.out.println("vShiftVec="+vShiftInvVec);
                        //vShiftVec.add(vShiftInvVec);
                        vVec.add(vShiftVec);
                        System.out.println("vVec after projecting "+vVec);
                        e.setPredictedPos(vVec);
                    }
                    else e.setPredictedPos(null); // Hide predicted sphere
                }
            }
            else if (count>=2 && (ny==1 || nx==1)){
                //fetch grid indices of first two defined electrodes
                int[] g1tmp = electrodeList.get(nrDefined[0]).xy;
                int[] g2tmp = electrodeList.get(nrDefined[1]).xy;
                //store 2D grid positions in 3D vectors g1 and g2
                // using homogeneous coordinates, allowing us to capture rotation
                // and translation via one transformation matrix
                Vec g1 = new Vec(g1tmp[0], g1tmp[1],1);
                Vec g2 = new Vec(g2tmp[0], g2tmp[1],1);
                //store electrode positions in vectors v1 and v2
                Vec v1 = electrodes[g1tmp[0]][g1tmp[1]].getPredictedPos();
                Vec v2 = electrodes[g2tmp[0]][g2tmp[1]].getPredictedPos();
                //define a difference vector
                System.out.println(nrDefined[0]);
                System.out.println(nrDefined[1]);
                double nrDiff = 1/(nrDefined[1]-(double)nrDefined[0]);
                System.out.println(nrDiff);
                Vec d21 = new Vec(v2);
                d21.sub(v1);
                d21.dot(d21,nrDiff);
                //retrieve the other electrode positions using d21
                Vec vVec = new Vec(d21);
                vVec.dot(0-(nrDefined[0]+1));
                vVec.add(v1);
                for(Electrode e : electrodeList) {
                    System.out.println(e.xy[0] + "" + e.xy[1]);
                    vVec.add(vVec,d21);
                    e.setPredictedPos(vVec);
                    // System.out.println("extrapolate vector: "+vVec);
                }                                                                   
            }                                                                   
        }
        catch (VecException ve) { 
            System.out.println(ve); 
        }
        catch (MatException me) { 
            System.out.println(me); 
        }
    }
    boolean fineTune(int i) throws MatException, VecException { 
        System.out.println("Fine tune electrode "+i);
        double delta=deltaStep;
        Electrode e=electrodeList.get(i);
        boolean cont=true;
        boolean newValue=false;
        while (cont) {
            cont=false;
            double distOrig=distFunc();
            System.out.println("distOrig="+distOrig);
            Vec v=e.getPredictedPos();
            for(int j=0;j<3;j++) {
                boolean contHigh;
                do {
                    contHigh=false;
                    // try higher value
                    v.data[j]+=delta;
                    e.setPredictedPos(v);
                    double dist=distFunc();
                    if (dist<distOrig) {
                    System.out.println("dist="+dist);
                    newValue=true;
                    distOrig=dist;
                    contHigh=true;
                    cont=true;
                    } else {
                    v.data[j]-=delta;
                    e.setPredictedPos(v);
                    }
                } while (contHigh);
                boolean contLow;
                do {
                    contLow=false;
                    // try lower value
                    v.data[j]-=delta;
                    e.setPredictedPos(v);
                    double dist=distFunc();
                    if (dist<distOrig) {
                        System.out.println("dist="+dist);
                        newValue=true;distOrig=dist;
                        contLow=true;
                        cont=true;
                    } else {
                        v.data[j]+=delta;
                        e.setPredictedPos(v);
                    }
                } while (contLow);
            }                                               
        }
        if (newValue) System.out.println("Updating electrode "+i);
        return newValue; 
    }
    void pertubate(int i) throws MatException, VecException {
        System.out.println("Pertubate electrode "+i);
        double delta=deltaStep;
        Electrode e=electrodeList.get(i);
        Vec v=e.getPredictedPos();
        Vec w=new Vec(v);
        System.out.println("X:");
        for(double x=-10*delta+v.getX();x<=10*delta+v.getX();x+=delta) {
            w.setX(x);
            e.setPredictedPos(w);
            System.out.println(x+" "+distFunc());
        }
        w.set(v);
        System.out.println("Y:");
        for(double y=-10*delta+v.getY();y<=10*delta+v.getY();y+=delta) {
            w.setY(y);
            e.setPredictedPos(w);
            System.out.println(y+" "+distFunc());
        }
        w.set(v);
        System.out.println("Z:");
        for(double z=-10*delta+v.getZ();z<=10*delta+v.getZ();z+=delta) {
            w.setZ(z);
            e.setPredictedPos(w);
            System.out.println(z+" "+distFunc());
        }
        e.setPredictedPos(v);
    }
    public void FineTuneByEnergyOptimization() {
        System.out.println("FineTuneByEnergyOptimization");
        System.out.println("volume="+volume);
        // Prepare volumes
        //volume.writeIcs("ext1.ics");
        brainV=new Volim("brainV");
        brainV.set(volume);
        if (brainV.getMax()>1) brainV.threshold(64);
        brainV.closing(10);
        //brainV.writeIcs("ext2.ics");
        Volim temp=new Volim("temp");
        temp.set(brainV);
        temp.erosion(brainV,1);
        //temp.writeIcs("ext3.ics");
        temp.sub(brainV);
        //temp.writeIcs("ext4.ics");
        temp.add(1);
        //temp.writeIcs("ext5.ics");
        temp.convert(types.VOXELTYPE.SINT16);
        distanceV.convert(types.VOXELTYPE.SINT16);
        distanceV.euclideanDistance(temp);
        //distanceV.writeIcs("distanceV.ics");
        boolean allPredicted=true;
        for(int i=0;i<electrodeList.size();i++) {
            Electrode e=electrodeList.get(i);
            if (e.getPredictedPos()==null) allPredicted=false;
        }
        if (!allPredicted) FitGridByPointMatching(); // Perform coarse prediction
        try {
            // Second perform fine tuning
            boolean cont=true;
            while (cont) {
                cont=false;
                for(int i=0;i<nx*ny;i++) {
                    cont|=fineTune(i);
                }
            }
        }
        catch (VecException ve) { 
            System.out.println(ve); 
        }
        catch (MatException me) { 
            System.out.println(me); 
        }
    }
    
    public void FitGridByPointMatching() {
        PointSet ps1=new PointSet();
        PointSet ps2=new PointSet();
        for(Integer i : electrodesDefined()) {
            Electrode el=electrodeList.get(i.intValue());
            ps1.add(new Vec(el.xy[0]*10, el.xy[1]*10, 0));
            ps2.add(el.getUserPos());
        }
        System.out.println("ps1="+ps1);
        System.out.println("ps2="+ps2);
        try {
            Mat m=ps1.calcRigidTransformation(ps2);
            System.out.println("m="+m);
            for(int y=0;y<ny;y++) {
                for(int x=0;x<nx;x++) {
                    Vec v=new Vec(x*10, y*10, 0, 1);
                    Vec w=new Vec(4);
                    w.dot(m, v);
                    w.changeDim(3);
                    Electrode el=getElectrode(x,y);
                    el.setPredictedPos(w);
                }
            }
        }
        catch (VecException ve) {
            System.out.println("VecException: "+ve);
        }
        catch (MatException me) {
            System.out.println("MatException: "+me);
        }
    }
    public double distFunc() throws VecException {
        double distU=distToUser();
        double distM=distMutual();
        double distB=distBend();
        double distC=distToCortex(divisionCortex);
        System.out.println("user="+distU+", mutual="+distM+", bend="+distB+", cortex="+distC);
        return weightDistUser*distU+weightDistMutual*distM+weightDistBend*distB+weightDistCortex*distC;
    }
    public double distToUser() throws VecException {
        // Calculate distance to user defined objects
        double dist=0;
        for(Integer i : electrodesDefined()) {
            Electrode e=electrodeList.get(i.intValue());
            Vec d=e.getUserPos();
            d.sub(e.getPredictedPos());
            dist+=d.length();
        }
        dist/=electrodesDefined().size();
        // System.out.println("distToUser="+dist);
        return dist;
    }
    public double distMutual() throws VecException {
        // Calculate distance between predicted grid positions
        // They should be 10 mm apart and 10*sqrt2 mm apart diagonnally
        double dist=0;
        int count=0;
        double distReq=10;
        // Check x-distances
        for(int y=0;y<ny;y++) {
            for(int x=0;x<nx-1;x++) {
                Electrode e1=electrodes[x][y];Electrode e2=electrodes[x+1][y];
                Vec d=e1.getPredictedPos();
                d.sub(e2.getPredictedPos());
                double diff=d.length()-distReq;
                // dist+=Math.abs(diff);
                dist+=diff*diff;
                count++;
            }
        }
        // Check y-distances
        for(int x=0;x<nx;x++) {
            for(int y=0;y<ny-1;y++) {
                Electrode e1=electrodes[x][y];
                Electrode e2=electrodes[x][y+1];
                Vec d=e1.getPredictedPos();
                d.sub(e2.getPredictedPos());
                double diff=d.length()-distReq;
                // dist+=Math.abs(diff);
                dist+=diff*diff;
                count++;
            }
        }
        // Check diagonals
        distReq=10*Math.sqrt(2);
        for(int x=0;x<nx-1;x++) {
            for(int y=0;y<ny-1;y++) {
                Electrode e1=electrodes[x][y];
                Electrode e2=electrodes[x+1][y+1];
                Vec d=e1.getPredictedPos();
                d.sub(e2.getPredictedPos());
                double diff=d.length()-distReq;
                // dist+=Math.abs(diff);
                dist+=diff*diff;
                count++;
                e1=electrodes[x+1][y];
                e2=electrodes[x][y+1];
                d=e1.getPredictedPos();
                d.sub(e2.getPredictedPos());
                diff=d.length()-distReq;
                // dist+=Math.abs(diff);
                dist+=diff*diff;
                count++;
            }
        }
        dist/=count;
        // dist=Math.sqrt(dist/count);
        // System.out.println("distMutual="+dist);
        return dist;
    }
    public double distBend() throws VecException {
        // Calculate bending energy between two consecutive segments
        double dist=0;
        int count=0;
        // Calculate bending energy in x-direction
        for(int y=0;y<ny;y++) {
            for(int x=1;x<nx-1;x++) {
                Electrode e1=electrodes[x-1][y];
                Electrode e2=electrodes[x][y];
                Electrode e3=electrodes[x+1][y];
                Vec v2=e2.getPredictedPos();
                Vec v1=new Vec(v2);
                v1.sub(v2, e1.getPredictedPos());
                v2.sub(e3.getPredictedPos(), v2);
                dist+=1-v1.dot(v2)/(v1.length()*v2.length());
                count++;
            }
        }
        // Calculate bending energy in y-direction
        for(int x=0;x<nx;x++) {
            for(int y=1;y<ny-1;y++) {
                Electrode e1=electrodes[x][y-1];
                Electrode e2=electrodes[x][y];
                Electrode e3=electrodes[x][y+1];Vec v2=e2.getPredictedPos();
                Vec v1=new Vec(v2);
                v1.sub(v2, e1.getPredictedPos());
                v2.sub(e3.getPredictedPos(), v2);
                dist+=1-v1.dot(v2)/(v1.length()*v2.length());
                count++;
            }
        }
        dist/=count;
        // System.out.println("distBend="+dist);
        return dist;
    }
    private double getCortexDistance(Vec pos) throws VecException {
        double dist;
        int dim[]=distanceV.getDimensions();
        double voxel[]=distanceV.getVoxelDimensions();
        Vec dataPos=new Vec();
        for(int i=0;i<3;i++) {
            dataPos.setV(i, pos.getV(i)/voxel[i]+dim[i]/2);
        }
        if (distanceV.contains(dataPos.data)) {
            dist=distanceV.getVal(dataPos,2);
        }
        else {
            // Point lies outside volume
            // Calculate distance to center of volume
            dist=pos.length();
        }
        return dist;
    }
    public double distToCortex(int division) throws VecException {
        double dist1=0;
        double dist2=0;
        int cd=1+division;
        int count=0;
        for(int y=0;y<=(ny-1)*cd;y++) {
            for(int x=0;x<=(nx-1)*cd;x++) {
                int ix=x/cd;
                int iy=y/cd;
                double dx=(x-ix*cd)/((double) cd);
                double dy=(y-iy*cd)/((double) cd);
                int ixh=ix+1;
                if (ixh>=nx) ixh=nx-1;
                int iyh=iy+1;
                if (iyh>=ny) iyh=ny-1;
                Electrode ell=electrodes[ix][iy];
                Electrode elh=electrodes[ix][iyh];
                Electrode ehl=electrodes[ixh][iy];
                Electrode ehh=electrodes[ixh][iyh];
                dist2+=(1-dx)*(1-dy)*getCortexDistance(ell.getPredictedPos())+(1-dx)*dy*getCortexDistance(elh.getPredictedPos())+dx*(1-dy)*getCortexDistance(ehl.getPredictedPos())+ dx*dy*getCortexDistance(ehh.getPredictedPos());
                count++;
            }
        }
        dist2/=count;
        // System.out.println("distToCortex2="+dist2);
        return dist1+dist2;
    }
    public void setProjectToBrain(boolean b){
        projectToBrain=b;
    }
    public boolean getProjectToBrain() {
        return projectToBrain;
    }
    public void setWeights(double w1, double w2, double w3, double w4) {
        weightDistUser=w1;
        weightDistMutual=w2;
        weightDistBend=w3;
        weightDistCortex=w4;
    }
    public double[] getWeights() {
        return new double[]{weightDistUser, weightDistMutual,
        weightDistBend, weightDistCortex};
    }
    public void setDeltaStep(double d) {
        deltaStep=d;
    }
    public void setDivisionCortex(int d) {
        divisionCortex=d;
    }
    public int getDivisionCortex() {
        return divisionCortex;
    }
}
