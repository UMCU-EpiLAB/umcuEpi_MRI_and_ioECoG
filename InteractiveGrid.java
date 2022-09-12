package interactivegrid2;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.text.NumberFormat;
import javax.swing.text.NumberFormatter;
import types.*;
import components.*;
import general.*;
import render.*;
import jni.*;
import utils.*;
import graphic.*;
import graphic3d.*;
import register.*;
import control.*;
import morphsegm.*;
import mansegm.*;

public class InteractiveGrid extends StepFrame implements ActionListener, ChangeListener {
    protected String MRNAME="mr.ics";
    protected String BRAINNAME="brain.ics";
    protected String CTNAME="ct";
    protected String LESIONNAME="lesion.ics";
    protected Plane3D mriPlane=null, brainPlane=null;
    protected Plane3D ctPlane=null, lesionPlane=null;
    protected Viewer3D resultView=null;
    protected Plane3D resultPlane=null;
    public Volim mriVolim, brainVolim, ctVolim, lesionVolim;
    public Volim ctViewVolim, brainViewVolim;
    //public Volim pOb=new Volim("Grid.pOb");
    //public Volim pL=new Volim("Grid.pL");
    //public Volim pI=new Volim("Grid.pI");
    //public ObjectStruct os=new ObjectStruct();
    protected InteractiveMix im;
    protected boolean CalcResampledVolume=true;
    protected Mat BackupExtraMatrix=null;
    protected PromptDialog waitDialog=null;
    protected LineGraphic CrossHairLines[]=new LineGraphic[4];
    protected JLabel mriLabel, brainLabel, ctLabel, lesionLabel;
    protected int initwidth=400, initheight=400;
    protected JRadioButton rbhb, rbhl, rbhr;
    protected JComplexSlider sViewDepth=null, sThreshold = new JComplexSlider("Threshold",1700,7000,0, 3500., "CT threshold", this, null);
    protected JCheckBox cbShowGrid, cbShowLesion;
    protected int nDilations=4;
    protected int midLinePos=-1;
    protected static final int HEMI_BOTH=1, HEMI_LEFT=2, HEMI_RIGHT=3;
    protected JCheckBox cbOrigGradient;
    protected int HemisphereMode=HEMI_BOTH;
    
    protected JLabel statusLabel;
    protected Color redColor=new Color(1.0f, 0.2f, 0.2f);
    protected Color blueColor=new Color(0.2f, 0.2f, 0.5f);
    protected String validVolumes="<html><center>VALID<br>VOLUMES</center></html>";
    protected String invalidVolumes="<html><center>INVALID<br>VOLUMES</center></html>";
    
    //suus start 31-07-2014: correct x and y grid dimensions and add a 5x4 grid
    //protected int GridTypeDimensions[][]={ {2,2}, {1,4}, {2,4}, {4,4}, {1,8}, {2,8},{4,8}, {8,8} };
    protected int GridTypeDimensions[][]={ {4,2}, {5,4}, {8,1},{8,2}, {8,4}, {8,6}, {8,8} };
    //suus end
    protected JComboBox<String> cbGridTypes=new JComboBox<String>();
    public    Vector<Grid> GridList=new Vector<Grid>();
    protected JComboBox<String> cbGrids=new JComboBox<String>();
    protected JFormattedTextField tfFirstNumber=null;

    protected JCheckBox cbShowNumbers=null;
    protected JButton bShowHideExtrapolation=null;

    protected JComboBox<String> cbElectrode=new JComboBox<>();
    protected JCheckBox cbElectrodeDefined=null;
    protected int SelectedGrid=-1, SelectedElectrode=-1;
    protected boolean ElectrodeDefined=false;
    protected boolean updateLock=false, allowActionPerformed=true;

    public InteractiveGrid(DeskTop d) {
        super(d,2,true,true);
        defaultTitle="Interactive grid localisation";
        
        for(int i=0; i<4; i++) CrossHairLines[i]=new LineGraphic();
        
        int dim[]=new int[] {10,10,10};
        mriVolim=new Volim("Grid.mriVolim", dim, VOXELTYPE.UINT8, 0.);
        brainVolim=new Volim("Grid.brainVolim", dim, VOXELTYPE.UINT8, 0.);
        ctVolim=new Volim("Grid.ctVolim", dim, VOXELTYPE.UINT8, 0.);
        lesionVolim=new Volim("Grid.lesionVolim", dim, VOXELTYPE.UINT8, 0.);
        ctViewVolim=new Volim("Grid.ctViewVolim", dim, VOXELTYPE.UINT8, 0.);
        brainViewVolim=new Volim("Grid.brainViewVolim",dim, VOXELTYPE.UINT8, 0.);
        
        InputView(0);
        volumeUpdate();
        ResultView(1);
        displayScreen(0);
        co.writeLine("Ready Interactive Grid.");
    }
    public void InputView(int screen) {
        JPanel ip = new JPanel();
        ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));
        
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setAlignmentY(Component.TOP_ALIGNMENT);
        ip.add(pane);
        // Adding slice views
        JPanel pane1 = addPanel(BoxLayout.X_AXIS);
        pane.add(pane1);
        mriLabel = new JLabel("MRI", JLabel.CENTER);
        mriPlane = addPlane3D(pane1, mriLabel, mriVolim,initwidth, initheight, SLICETYPE.XY);
        brainLabel = new JLabel("BRAIN", JLabel.CENTER);
        brainPlane = addPlane3D(pane1, brainLabel, brainVolim,initwidth, initheight, SLICETYPE.XY);
        pane1 = addPanel(BoxLayout.X_AXIS);
        pane.add(pane1);
        ctLabel = new JLabel("CT", JLabel.CENTER);
        ctPlane = addPlane3D(pane1, ctLabel, ctVolim,initwidth, initheight, SLICETYPE.XY);
        lesionLabel = new JLabel("LESION", JLabel.CENTER);
        lesionPlane = addPlane3D(pane1, lesionLabel, lesionVolim,initwidth, initheight, SLICETYPE.XY);
        // Adding buttons and sliders
        Box box=new Box(BoxLayout.Y_AXIS);
        box.setAlignmentY(Component.TOP_ALIGNMENT);
        ip.add(box);
        JPanel p=new OvalPanel("Input");
        box.add(p);
        JButton blr=new JButton("Load mri");
        blr.addActionListener(this);
        p.add(blr);
        JButton blf=new JButton("Load brain");
        blf.addActionListener(this);
        p.add(blf);
        JButton bli=new JButton("Load ct");
        bli.addActionListener(this);
        p.add(bli);
        JButton blii=new JButton("Load lesion");
        blii.addActionListener(this);
        p.add(blii);
        p=new OvalPanel("Operations");box.add(p);
        JButton brs=new JButton("Segment brain");
        brs.addActionListener(this);
        p.add(brs);
        JButton bri=new JButton("Register ct");
        bri.addActionListener(this);
        p.add(bri);
        JButton brdl=new JButton("Segment lesion");
        brdl.addActionListener(this);
        p.add(brdl);
        statusLabel=new JLabel(invalidVolumes);
        utils.setBlueFont(statusLabel);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(statusLabel);
        ViewVec.get(screen).add(mriPlane);
        ViewVec.get(screen).add(brainPlane);
        ViewVec.get(screen).add(ctPlane);
        ViewVec.get(screen).add(lesionPlane);
        PanelVec[screen]=ip;
        TitleVec[screen]=defaultTitle;
        DescriptionVec[screen]="elektrode_page1.html";
    }
    public void ResultView(int screen) {
        JPanel ip = new JPanel();
        ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        pane.setAlignmentY(Component.TOP_ALIGNMENT);
        ip.add(pane);
        // Adding slice and 3D views
        resultView = addViewer3D(pane, new JLabel("3D"), brainViewVolim,initwidth, initheight);
        resultView.getWorld().setNumberOfGroups(2);
        JPanel p2= addPanel(BoxLayout.Y_AXIS);
        resultPlane=new Plane3D(resultView.getWorld()) {
            public void objectsChanged() {
                //System.out.println("resultView plane objectsChanged.");
                gridObjectsChanged();
            }
        };
        resultPlane.setImageSize(initwidth, initheight);
        p2.setAlignmentY(Component.TOP_ALIGNMENT);
        resultPlane.setName("Slice");
        p2.add(resultPlane);
        JLabel l=new JLabel("Slice");
        utils.setBlueFont(l);
        p2.add(l);
        pane.add(p2);
        // Adding buttons and sliders
        Box box=new Box(BoxLayout.Y_AXIS);
        box.setAlignmentY(Component.TOP_ALIGNMENT);
        ip.add(box);
        JPanel p=new OvalPanel("View");
        box.add(p);
        cbShowGrid=new JCheckBox("Grid",!ctLabel.getText().equals("<html><center>ct<br>NOMATCH</center></html>"));
        p.add(cbShowGrid);
        cbShowGrid.addActionListener(this);
        cbShowLesion=new JCheckBox("Lesion",!lesionLabel.getText().equals("<html><center>NOLESION</center></html>"));
        p.add(cbShowLesion);
        cbShowLesion.addActionListener(this);
        JCheckBox cbDistance=new JCheckBox("Distance", true);
        p.add(cbDistance);
        cbDistance.addActionListener(this);sViewDepth = new JComplexSlider("View [mm]",5, 20, 2, 10., "ViewDepth", this, null);
        sViewDepth.add2(p);
        sViewDepth.setMode(JComplexSlider.MODE.REL_MOVE);
        sViewDepth.setAbsMinMax(1e-30, 1e30);
        sThreshold.add2(p);
        sThreshold.setMode(JComplexSlider.MODE.REL_MOVE);
        sThreshold.setAbsMinMax(10, 10000);
        JPanel dilatePanel = new JPanel();
        dilatePanel.setLayout(new BoxLayout(dilatePanel, BoxLayout.X_AXIS));
        dilatePanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JLabel ld = new JLabel("#layers");
        String maskLayerTip="Extra layers around brain where grid may be present";
        ld.setToolTipText(maskLayerTip);
        if (nDilations==-1) nDilations=4;
        JSpinner ds = new JSpinner();
        ds.setMaximumSize(new Dimension(40,20));
        ds.setModel(new SpinnerNumberModel(nDilations, 0,null,1));
        ds.setEnabled(true);
        ds.addChangeListener(this);
        ds.setToolTipText(maskLayerTip);
        dilatePanel.add(ld);
        dilatePanel.add(Box.createRigidArea(new Dimension(10,1)));
        dilatePanel.add(ds);
        p.add(dilatePanel);
        cbOrigGradient=new JCheckBox("Gradient from MR", true);
        cbOrigGradient.setActionCommand("gradient");
        cbOrigGradient.addActionListener(this);
        cbOrigGradient.setToolTipText("Changing this may increase quality");
        p.add(cbOrigGradient);
        p=new OvalPanel("Show hemisphere");
        box.add(p);
        rbhb=new JRadioButton("Both", true);
        rbhb.addActionListener(this);
        rbhb.setActionCommand("HemisphereBoth");
        p.add(rbhb);
        rbhl=new JRadioButton("Left");
        rbhl.addActionListener(this);
        rbhl.setActionCommand("HemisphereLeft");
        p.add(rbhl);
        rbhr=new JRadioButton("Right");
        rbhr.addActionListener(this);
        rbhr.setActionCommand("HemisphereRight");
        p.add(rbhr);
        ButtonGroup bg=new ButtonGroup();
        bg.add(rbhb);
        bg.add(rbhl);
        bg.add(rbhr);
        p.add(new JLabel("Shift midline"));
        JPanel p1=new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p1.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(p1);
        JButton bLeftLeft=new JButton("<<");
        bLeftLeft.setMaximumSize(new Dimension(30,20));
        bLeftLeft.setMargin(new Insets(0,0,0,0));
        bLeftLeft.addActionListener(this);
        p1.add(bLeftLeft);
        JButton bLeft=new JButton("<");
        bLeft.setMaximumSize(new Dimension(30,20));
        bLeft.setMargin(new Insets(0,0,0,0));
        bLeft.addActionListener(this);
        p1.add(bLeft);
        JButton bRight=new JButton(">");
        bRight.setMaximumSize(new Dimension(30,20));
        bRight.setMargin(new Insets(0,0,0,0));
        bRight.addActionListener(this);
        p1.add(bRight);
        JButton bRightRight=new JButton(">>");
        bRightRight.setMaximumSize(new Dimension(30,20));
        bRightRight.setMargin(new Insets(0,0,0,0));bRightRight.addActionListener(this);
        p1.add(bRightRight);
        p=new OvalPanel("Grid");
        box.add(p);
        Vector<String> GridNames=new Vector<String>();
        for(int i=0;i<GridTypeDimensions.length;i++) {
            int nx=GridTypeDimensions[i][0];
            int ny=GridTypeDimensions[i][1];
            GridNames.add(nx+"x"+ny);
        }
        cbGridTypes.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cbGridTypes);
        cbGridTypes.setModel(new DefaultComboBoxModel<String>(GridNames));
        JButton bNewGrid=new JButton("New");
        bNewGrid.setActionCommand("New grid");
        bNewGrid.addActionListener(this);
        p.add(bNewGrid);
        cbGrids.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbGrids.setActionCommand("Select grid");
        cbGrids.addActionListener(this);
        p.add(cbGrids);
        JButton bDeleteGrid=new JButton("Delete");
        bDeleteGrid.setActionCommand("Delete grid");
        bDeleteGrid.addActionListener(this);
        p.add(bDeleteGrid);
        JButton bLoadGrid=new JButton("Read");
        bLoadGrid.setActionCommand("Read grid");
        bLoadGrid.addActionListener(this);
        p.add(bLoadGrid);
        JButton bSaveGrid=new JButton("Save");
        bSaveGrid.setActionCommand("Save grid");
        bSaveGrid.addActionListener(this);
        p.add(bSaveGrid);
        JButton bExtrapolateGrid1=new JButton("Fit grid");
        bExtrapolateGrid1.setToolTipText("Fit grid by point matching");
        bExtrapolateGrid1.addActionListener(this);
        p.add(bExtrapolateGrid1);
        JButton bExtrapolateGrid2=new JButton("Fine tune");
        bExtrapolateGrid2.setToolTipText("Finetune points by energy optimization");
        bExtrapolateGrid2.addActionListener(this);
        p.add(bExtrapolateGrid2);
        JButton bExtrapolateGrid3=new JButton("3 point extrap+proj");
        bExtrapolateGrid3.setToolTipText("Three point extrapolation and point projection");
        bExtrapolateGrid3.addActionListener(this);
        p.add(bExtrapolateGrid3);
        JButton bPertubate=new JButton("Pertubate");
        bPertubate.addActionListener(this);
        p.add(bPertubate);
        JButton bShowInfoGrid=new JButton("Show info");
        bShowInfoGrid.setActionCommand("Show info grid");
        bShowInfoGrid.addActionListener(this);
        p.add(bShowInfoGrid);
        cbShowNumbers=new JCheckBox("Show numbers", true);
        cbShowNumbers.addActionListener(this);
        p.add(cbShowNumbers);
        bShowHideExtrapolation=new JButton("Show/hide extrap");
        bShowHideExtrapolation.addActionListener(this);
        p.add(bShowHideExtrapolation);
        p=new OvalPanel("Electrode");
        box.add(p);
        JPanel firstNumberPanel = new JPanel();
        p.add(firstNumberPanel);
        firstNumberPanel.setLayout(new BoxLayout(firstNumberPanel,BoxLayout.X_AXIS));
        firstNumberPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        firstNumberPanel.add(new JLabel("First number "));
        NumberFormat format = NumberFormat.getInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(0);
        formatter.setMaximum(Integer.MAX_VALUE);
        formatter.setAllowsInvalid(false);
        // If you want the value to be committed on each keystroke instead of focus lost
        //formatter.setCommitsOnValidEdit(true);
        tfFirstNumber = new JFormattedTextField(formatter);
        tfFirstNumber.setValue(1); // new Integer(1));
        firstNumberPanel.add(tfFirstNumber);
        tfFirstNumber.setActionCommand("First number");
        tfFirstNumber.addActionListener(this);
        p.add(new JLabel("Number"));
        cbElectrode.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cbElectrode);
        // ElectrodeComboBox.setModel(new DefaultComboBoxModel<Integer>(GridNames));
        cbElectrode.setActionCommand("ElectrodeComboBox");
        cbElectrode.addActionListener(this);
        cbElectrodeDefined=new JCheckBox("Defined");
        cbElectrodeDefined.addActionListener(this);
        p.add(cbElectrodeDefined);
        p=new OvalPanel("Generate");
        box.add(p);
        JButton bGenerate=new JButton("Generate views");
        bGenerate.addActionListener(this);
        p.add(bGenerate);
        // box.validate();
        ViewVec.get(screen).add(resultView);
        ViewVec.get(screen).add(resultPlane);
        PanelVec[screen]=ip;
        TitleVec[screen]=defaultTitle;
        DescriptionVec[screen]="elektrode_page2.html";
        control.Properties props=desktop.global.co.vc.getProps();
        props.read(resultView.getWorld().getChannel(0),desktop.global.pio.getDirectory()+"brain.txt");
        props.read(resultView.getWorld().getChannel(1),desktop.global.pio.getDirectory()+"eleks.txt");
        resultView.update();
    }
    public void showDataIso(Volim data, Volim iso,boolean showData,boolean showIso, int n, Plane3D p) {
        if (showIso) {
            iso.isoLines(data, n);
            if (showData) {
                iso.convert(data.getType());
                iso.mul(data.getMax()*0.5);
                iso.add(data);
            }
            p.setVolume(iso);
        }
        else p.setVolume(data); // p.setVolume(showData?data:null);
        p.update();
    }
    public void actionPerformed(ActionEvent ae) {
        //System.out.println("actionPerformed, ae="+ae);
        if (!allowActionPerformed) return;
        World world=resultView.getWorld();
        if (ae.getSource()!=validationDialog && desktop.getWaitForInput()) return;
        if (super.ProcessAction(ae)) return;
        String command=ae.getActionCommand();System.out.println("Command="+command);
        if (command.equals("objectsChanged")) gridObjectsChanged();
        if (command.equals("Load mri")) {
            readVolim(mriVolim, mriPlane);
            desktop.global.volume=mriVolim;
            MRNAME=mriVolim.getFilename();
            File f=new File(mriVolim.getFilename());
            mriLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>");
            calcVolims();
        }
        else if (command.equals("Load brain")) {
            readVolim(brainVolim, brainPlane);
            BRAINNAME=brainVolim.getFilename();
            File f=new File(brainVolim.getFilename());
            brainLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>
            ");
            midLinePos=brainVolim.getSize(0)/2;
            brainViewVolim.set(brainVolim);
            calcVolims();
        }
        else if (command.equals("Load ct")) {
            readVolim(ctVolim, ctPlane);
            File f=new File(ctVolim.getFilename());
            CTNAME=utils.stripExtension(f.getName());
            ctLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>");
            calcVolims();
        }
        else if (command.equals("Load lesion")) {
            readVolim(lesionVolim, lesionPlane);
            LESIONNAME=lesionVolim.getFilename();
            File f=new File(lesionVolim.getFilename());
            lesionLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>");
            calcVolims();
        }
        else if (command.equals("Recon")) {
            volumeUpdate();
        }
        else if (command.equals("gradient")) {
            Channel chan=resultView.getWorld().getChannel(0);
            chan.setGradient(cbOrigGradient.isSelected()?mriVolim:null);
            updateAll();
        }
        else if (command.equals("Segment brain")) {
            MorphSegm ms = new MorphSegm(desktop);
            ms.addActionListener(this,"Recon"); // Let the segmentation module call the elektrode module to update the images after brain registration
            ms.setDefaultFilename("brain.ics");
            ms.showDialog();
        }
        else if (command.equals("Register ct")) {
            Register r = new Register(desktop, false, false);
            r.addActionListener(this,"Recon"); // Let the register module call the elektrode module to update the images after an image registration
            r.setRef(mriVolim);
            r.setFloat(ctVolim);
            r.setDefaultFilename(ctVolim.getFilename());
            r.showDialog();
        }
        else if (command.equals("Segment lesion")) {
            ManSegm ms = new ManSegm(desktop, lesionVolim);
            ms.addActionListener(this,"Recon"); // Let the register module call the elektrode module to update the images after an image registration
            ms.setDefaultFilename(LESIONNAME);
            ms.showDialog();
        }
        else if (command.equals("Grid")) {
            // World world=resultView.getWorld();
            Channel chan1=world.getChannel(1);
            Volim v=ctViewVolim;
            if (!v.sameSizes(brainViewVolim) || !cbShowGrid.isSelected()) v=null;chan1.setData(v);
            resultView.update();
        }
        else if (command.equals("Lesion")) {
            // World world=resultView.getWorld();
            Channel chan0=world.getChannel(0);
            Volim v=lesionVolim;
            if (!v.sameSizes(brainViewVolim) || !cbShowLesion.isSelected()) v=null;
            chan0.setLabel(v);
            resultView.update();
        }
        else if (command.equals("Distance")) {
            // World world=resultView.getWorld();
            world.setCalcDistance(!world.getCalcDistance());
            resultView.update();
        }
        else if (command.equals("ViewDepth")) {
            int depth=(int) (sViewDepth.getValue()+0.5);
            // World world=resultView.getWorld();
            Channel chan0=world.getChannel(0);
            Material m=chan0.getMaterialList().get(0);
            m.setExci(depth, 1);
            m.setEmi(depth, 1);
            chan0.updateMaterial(0);
            resultView.update();
        }
        else if (command.equals("Threshold") || command.equals("CT threshold")) {
            calcVolims();
        }
        else if (command.equals("Generate views")) {
            String s="view";
            if (cbShowGrid.isSelected()) s+="+grid";
            if (cbShowLesion.isSelected()) s+="+lesion";
            if (HemisphereMode==HEMI_BOTH) {
                desktop.global.co.vc.setAllViewPrefix(s);
                desktop.global.DisplayImages.fireActionPerformed(desktop.global.co.vc,"all_views");
            }
            else {
                resultView.writeJpeg(s+File.separator+"interhemisphere.jpg");
            }
        }
        else if (command.equals("HemisphereBoth") && HemisphereMode!=HEMI_BOTH) {
            brainViewVolim.set(brainVolim);
            newBrainViewVolim();
            HemisphereMode=HEMI_BOTH;
        }
        else if (command.equals("HemisphereLeft") && HemisphereMode!=HEMI_LEFT && midLinePos>=0) {
            brainViewVolim.set(brainVolim);
            int offset[]=new int[] {0,0,0};
            int width[]= {midLinePos,brainVolim.getSize(1),brainVolim.getSize(2)};
            brainViewVolim.adaptPartImage(brainVolim,offset, width, offset, Volim.ARITH.XOR);
            newBrainViewVolim();
            HemisphereMode=HEMI_LEFT;
        }
        else if (command.equals("HemisphereRight") && HemisphereMode!=HEMI_RIGHT && midLinePos>=0) {
            brainViewVolim.set(brainVolim);
            int offset[]=new int[] {midLinePos,0,0};
            int width[]= {brainVolim.getSize(0)-midLinePos,brainVolim.getSize(1),brainVolim.getSize(2)};
            brainViewVolim.adaptPartImage(brainVolim,offset, width, offset, Volim.ARITH.XOR);
            newBrainViewVolim();
            HemisphereMode=HEMI_RIGHT;
        }
        else if (HemisphereMode!=HEMI_BOTH && midLinePos>=0) {
            if (command.equals(">")) moveMidLine(midLinePos+1);
            else if (command.equals(">>")) moveMidLine(midLinePos+5);
            else if (command.equals("<")) moveMidLine(midLinePos-1);
            else if (command.equals("<<")) moveMidLine(midLinePos-5);
            }
        else if (command.equals("New grid")) {
            String sizeString = String.valueOf(cbGridTypes.getSelectedItem());
            int posTime=sizeString.indexOf('x');int nx=Integer.valueOf(sizeString.substring(0,posTime));
            int ny=Integer.valueOf(sizeString.substring(posTime+1));
            System.out.println("nx="+nx+", ny="+ny);
            GridList.add(new Grid(resultView.getWorld(), nx, ny));
            SelectedGrid=GridList.size()-1;
            updateGridAndElectrodeInterfaces();
            changeGrid();
            desktop.global.co.pc.update(); // Update tree nodes
            /*
            SelectedElectrode=0;
            desktop.global.co.pc.update();
            updateGridAndElectrodeInterfaces();
            updateTextGraphics();
            resultView.update();
            */
        }
        else if (command.equals("Select grid") && SelectedGrid!=cbGrids.getSelectedIndex()) {
            changeGrid();
            /*
            SelectedGrid=cbGrids.getSelectedIndex();
            System.out.println("SelectedGrid="+SelectedGrid);
            SelectedElectrode=-1;
            ElectrodeDefined=false;
            render.InteractiveObject.exitSubEdit(resultView.getWorld());
            render.InteractiveObject.deselectAll(resultView.getWorld());
            GridList.get(SelectedGrid).getGroupObject().setSelected(true);
            render.InteractiveObject.enterSubEdit(resultView.getWorld());
            updateGridAndElectrodeInterfaces();
            */
        }
        else if (command.equals("Delete grid")) {
            if (SelectedGrid<0) return;
            Grid g=GridList.get(SelectedGrid);
            // Remove objects associated with this grid
            g.clear();
            SelectedElectrode=-1;
            ElectrodeDefined=false;
            GridList.remove(SelectedGrid);
            SelectedGrid=GridList.size()-1;
            changeGrid();
            desktop.global.co.pc.update(); // Update tree nodes
            //updateGridAndElectrodeInterfaces();
            //resultView.update();
        }
        else if (command.equals("Fit grid")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                Grid g=GridList.get(SelectedGrid);
                g.setVolume(brainVolim);
                g.FitGridByPointMatching();
                resultView.update();
                resultPlane.update();
                updateTextGraphics();
            }
        }
        else if (command.equals("Fine tune")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                Grid g=GridList.get(SelectedGrid);
                g.setVolume(brainVolim);
                g.FineTuneByEnergyOptimization();
                resultView.update();
                resultPlane.update();
                updateTextGraphics();
            }
        }
        else if (command.equals("3 point extrap+proj")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                Grid g=GridList.get(SelectedGrid);
                g.setVolume(brainVolim);
                g.FitGridAndProject();
                resultView.update();
                resultPlane.update();
                updateTextGraphics();
            }
        }
        else if (command.equals("Pertubate")) {
            try {
                if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                    Grid g=GridList.get(SelectedGrid);
                    for(Electrode el : g.getElectrodeList()) {
                        if (el.getPredictedObject().getSelected()) {
                            g.pertubate(g.getElectrodeList().indexOf(el));
                        }
                    }
                }
            }
            catch (MatException me) { System.out.println("Pertubate MatException"+me); }
            catch (VecException ve) { System.out.println("Pertubate VecException"+ve); }
            resultView.update();
            resultPlane.update();
            updateTextGraphics();
        }
        else if (command.equals("Show info grid")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                System.out.println(GridList.get(SelectedGrid));
            }
        }
        else if (command.equals("First number")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                Grid g=GridList.get(SelectedGrid);
                g.setFirstNumber(((Number)tfFirstNumber.getValue()).intValue());
                updateGridAndElectrodeInterfaces();
                updateTextGraphics();
                resultView.update();
            }
        }
        else if (command.equals("ElectrodeComboBox")) {
            SelectedElectrode=cbElectrode.getSelectedIndex();
            if (SelectedElectrode>=0) {
                if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                    Grid g=GridList.get(SelectedGrid);
                    Electrode e=g.getElectrodeList().get(SelectedElectrode);
                    System.out.println("SelectedGrid="+SelectedGrid);
                    System.out.println("SelectedElectrode="+SelectedElectrode);
                    System.out.println("user object: "+e.getUserObject());
                    boolean selected=e.getUserObject().getSelected();
                    System.out.println("selected="+selected);
                    cbElectrodeDefined.setSelected(e.getUserObject().getVisible());
                }
            }
        }
        else if (command.equals("Defined")) {
            int index=cbElectrode.getSelectedIndex();
            if (index>=0) {
                if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                    try {
                        Grid g=GridList.get(SelectedGrid);
                        Electrode e=g.getElectrodeList().get(index);
                        if (cbElectrodeDefined.isSelected()) {
                            Vec v=e.getUserPos();
                            if (v==null) {
                                // Point has not been defined yet
                                Vec sum=null;
                                int count=1;
                                for(Electrode e2 : g.getElectrodeList() ) {
                                    Vec w=e2.getUserPos();
                                    if (w!=null) {
                                        if (sum==null) sum=new Vec(w);
                                        else {
                                            sum.add(w);
                                            count++;
                                        }
                                    }
                                }
                                v=new Vec(0,0,0);
                                if (sum!=null) {
                                    v.div(sum, count);
                                }
                                e.setUserPos(v);
                            }
                            render.InteractiveObject.deselectAll(world);
                            e.getUserObject().setSelected(world, true);
                        }
                        else {
                            e.setUserPos(null); // Clear position
                        }
                        updateTextGraphics();
                        resultView.update();
                    }
                    catch (VecException ve) {
                        System.out.println("VecException, ve="+ve);
                    }
                }
            }
            updateElectrodeInterface();
        }
        else if (command.equals("Read grid")) {
            for(Grid g:GridList) {
                g.clear();
            }
            GridList.clear();
            String dir=desktop.global.pio.getPatientDir()+File.separatorChar;
            int i=0;
            boolean cont=true;
            while (cont) {
                Grid g=new Grid(resultView.getWorld());
                cont=g.read(dir+"grid"+i+".dat");
                if (cont) {
                    // Allocate corresponding user specified objects
                    for(Electrode e : g.getElectrodeList()) {
                        e.getUserObject().setVisible(e.getUserPos()!=null);
                    }
                    System.out.println("Grid after reading: "+g);
                    GridList.add(g);
                    i++;
                }
            }
            SelectedGrid=GridList.size()-1;
            SelectedElectrode=0;
            desktop.global.co.pc.update();
            updateGridAndElectrodeInterfaces();
            updateTextGraphics();
            resultView.update();
            changeGrid();
        }
        else if (command.equals("Save grid")) {
            String dir=desktop.global.pio.getPatientDir()+File.separatorChar;
            for(int i=0;i<GridList.size(); i++) {
                GridList.get(i).save(dir+"grid"+i+".dat");
            }    
        }
        else if (command.equals("Show numbers")) {
            updateTextGraphics();
            resultView.update();
            resultPlane.update();
        }
        else if (command.equals("Show/hide extrap")) {
            if (SelectedGrid>=0 && SelectedGrid<GridList.size()) {
                Grid g=GridList.get(SelectedGrid);
                for(Electrode e: g.getElectrodeList()) {
                    render.InteractiveObject o=e.getPredictedObject();
                    o.setVisible(!o.getVisible());
                }
            }
            updateTextGraphics();
            resultView.update();
            resultPlane.update();
        }
    }
    public void stateChanged(ChangeEvent e) {
        JSpinner spinner=(JSpinner) e.getSource();
        nDilations=((Integer) spinner.getValue()).intValue();
        calcVolims();
    }
    public void volumeUpdate() {
        String dir=desktop.global.pio.getPatientDir()+File.separatorChar;
        String s=dir+MRNAME;
        if (utils.isAbsolute(MRNAME)) s=MRNAME;
        if (desktop.global.volume!=null) {
            mriVolim.set(desktop.global.volume);
            MRNAME=desktop.global.volume.getFilename();
            File f=new File(desktop.global.volume.getFilename());
            mriLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>");
        }
        else if (utils.fileExists(s)) mriVolim.read(s);
        s=dir+BRAINNAME;
        if (utils.isAbsolute(BRAINNAME)) s=BRAINNAME;
        if (desktop.global.brain_volume!=null) {
            brainVolim.set(desktop.global.brain_volume);
            BRAINNAME=desktop.global.brain_volume.getFilename();
            File f=new File(desktop.global.brain_volume.getFilename());
            brainLabel.setText("<html><center>"+utils.stripExtension(f.getName())+"</center></html>");
        }
        else if (utils.fileExists(s)) {
            brainVolim.read(s);
            if (midLinePos<0) midLinePos=brainVolim.getSize(0)/2;
            // System.out.println("brainVolim="+brainVolim);
            // System.out.println("midLinePos="+midLinePos);
        }
        else brainVolim.threshold(mriVolim,mriVolim.getMax()/2);
        String fn=dir+CTNAME;
        if (utils.isAbsolute(CTNAME)) fn=CTNAME;
        s=fn+"_resampled.ics";
        if (cbShowGrid!=null) cbShowGrid.setSelected(true);
        if (utils.fileExists(s)) {
            ctVolim.read(s);
            ctLabel.setText("<html><center>ct_resampled</center></html>");
        }
        else {
            s=fn+"_registered.ics";
            if (utils.fileExists(s)) {
                ctVolim.read(s);
                ctLabel.setText("<html><center>ct_registered</center></html>");
            }
            else {
                s=fn+".ics";
                if (utils.fileExists(s)) {
                    ctVolim.read(s);
                    ctLabel.setText("<html><center>ct<br>NO MATCH</center></html>");
                    if (cbShowGrid!=null) cbShowGrid.setSelected(false);
                }
            }
        }
        s=dir+LESIONNAME;
        if (utils.isAbsolute(LESIONNAME)) s=LESIONNAME;
        if (utils.fileExists(s)) {
            lesionVolim.read(s);
            lesionLabel.setText("<html><center>LESION</center></html>");
        }
        else {
            lesionLabel.setText("<html><center>NO LESION</center></html>");
        }
        if (cbShowLesion!=null) cbShowLesion.setSelected(utils.fileExists(s));
        // Calculating volumes
        calcVolims();
        super.volumeUpdate();
    }
    public Plane3D addPlane3D(JPanel p1,JLabel t, Volim v, int w, int h, SLICETYPE o) {
        JPanel p2= addPanel(BoxLayout.Y_AXIS);
        Plane3D plane = new Plane3D(desktop);
        plane.setName(t.getText());
        plane.setVolume(v);
        plane.setImageSize(w,h);
        plane.setOrientation(o, false);
        // plane.setAutoFit(false);
        p2.add(plane);
        utils.setBlueFont(t);
        p2.add(t);p1.add(p2);
        return plane;
    }
    public Viewer3D addViewer3D(JPanel p1,JLabel t, Volim v, int width, int height) {
        JPanel p2= addPanel(BoxLayout.Y_AXIS);
        World world = World.getDefault(desktop);
        world.setMultipleGeometries(true);
        Channel chan0=world.getChannel(0);
        chan0.setData(brainViewVolim);
        chan0.setGradient(mriVolim);
        if (lesionVolim.sameSizes(brainViewVolim)) {
            System.out.println("Setting lesion"+lesionVolim);
            chan0.setLabel(lesionVolim);
        }
        Channel chan1=new Channel(world);
        chan1.setData(ctViewVolim.sameSizes(brainViewVolim)?ctViewVolim:null);
        //world.setCalcDistance(true);
        //world.setClipped(true);
        Viewer3D view = new Viewer3D(world) {
            // Override objectsChanged
            public void objectsChanged() {
                //System.out.println("InteractiveGrid objectsChanged.");
                gridObjectsChanged();
            }
        };
        view.setName(t.getText());
        view.setImageSize(width,height);
        view.setZoom(1.6);
        // world.setNumberOfGroups(2);
        double c[]=world.getBackgroundProperties();
        c[0]=0;
        world.setBackgroundProperties(c);
        c=world.getTableProperties();
        c[3]=0;
        world.setTableProperties(c);
        p2.add(view);
        utils.setBlueFont(t);
        p2.add(t);
        p1.add(p2);
        return view;
    }
    public JPanel addPanel(int axis) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, axis));
        p.setAlignmentY(Component.TOP_ALIGNMENT);
        return p;
    }
    public void readVolim(Volim v, Plane3D p) {
        v.read("?");
        double voxel[]=v.getVoxelDimensions();
        int dim[]=v.getDimensions();
        p.setZoom(256./dim[0]/voxel[0]);
        p.setZoom(1.);
        p.center();
    }
    public boolean equalVolumes(Volim v1, Volim v2) {
        return v1.sameSizes(v2) && v1.sameVoxelDimensions(v2);
    }
    public void calcVolims() {
        boolean same1=equalVolumes(mriVolim, brainVolim);
        utils.setFontAndColor(brainLabel, same1?blueColor:redColor);
        boolean same2=equalVolumes(mriVolim, ctVolim);
        utils.setFontAndColor(ctLabel, same2?blueColor:redColor);
        statusLabel.setText((same1 && same2?validVolumes:invalidVolumes));
        /*
        if (!equalVolumes(mriVolim, lesionVolim)) {
            // Copy mriVolim to lesionVolim
            lesionVolim.convert(mriVolim, VOXELTYPE.UINT8);
            lesionVolim.set(0.);
        }
        */
        ctViewVolim.biThreshold(ctVolim, sThreshold.getValue(), 10000);ctViewVolim.mul(ctVolim);
        brainViewVolim.dilation(brainVolim, nDilations);
        ctViewVolim.mul(brainViewVolim);
        brainViewVolim.set(brainVolim);
        newBrainViewVolim();
    }
    public void newBrainViewVolim() {
        if (resultView==null) return;
        World world=resultView.getWorld();
        Channel chan0=world.getChannel(0);
        chan0.setData(brainViewVolim);
        chan0.setLabel((cbShowLesion.isSelected() && lesionVolim.sameSizes(brainViewVolim))?lesionVolim:null);
        Channel chan1=world.getChannel(1);
        Volim v=ctViewVolim;
        if (!v.sameSizes(brainViewVolim) || !cbShowGrid.isSelected()) v=null;
        chan1.setData(v);
        resultView.update();
    }
    public void moveMidLine(int pos) {
        if (pos<0) pos=0;
        if (pos>=brainViewVolim.getSize(0)) pos=brainViewVolim.getSize(0)-1;
        if (pos==midLinePos) return;
        int p1=pos;
        int p2=midLinePos;
        if (p1>p2) {
            p1=p2;
            p2=pos;
        }
        int offset[]=new int[] {p1,0,0};
        int width[]= {p2-p1,brainVolim.getSize(1),brainVolim.getSize(2)};
        // System.out.println("offset="+utils.WriteVec(offset));
        // System.out.println("width="+utils.WriteVec(width));
        brainViewVolim.adaptPartImage(brainVolim,offset, width, offset, Volim.ARITH.XOR);
        midLinePos=pos;
        newBrainViewVolim();
    }
    public void updateGridAndElectrodeInterfaces() {
        allowActionPerformed=false;
        Vector<String>GridNames=new Vector<String>();
        for(int i=0;i<GridList.size(); i++) {
            Grid g=GridList.get(i);
            GridNames.add((i+1)+": "+g.getName());
        }
        cbGrids.setModel(new DefaultComboBoxModel<String>(GridNames));
        // Set selection according to selected object
        cbGrids.setSelectedIndex(SelectedGrid);
        updateElectrodeInterface();
    }
    public void updateElectrodeInterface() {
        if (SelectedGrid<0 || SelectedGrid>=GridList.size()) {
            if (cbElectrode!=null) {
                cbElectrode.removeAllItems();
                cbElectrode.setEnabled(false);
            }
            if (cbElectrodeDefined!=null) cbElectrodeDefined.setEnabled(false);
        }
        else {
            Grid g=GridList.get(SelectedGrid);
            int nElectrodes=g.getNXY();
            int firstNumber=g.getFirstNumber();
            tfFirstNumber.setValue(firstNumber);
            Vector<String> electrodes=new Vector<>();
            for(int j=0;j<nElectrodes;j++) {
                boolean defined=g.getElectrodeList().get(j).getUserObject().getVisible();
                electrodes.add(""+(j+firstNumber)+ (defined?" defined":""));
            }
            cbElectrode.setModel(new DefaultComboBoxModel<String>(electrodes));
            System.out.println("SelectedElectrode="+SelectedElectrode);
            cbElectrode.setSelectedIndex(SelectedElectrode);
            cbElectrode.setEnabled(true);
            if (SelectedElectrode>=0) {
                cbElectrodeDefined.setSelected(g.getElectrodeList().get(SelectedElectrode).getU        serObject().getVisible());
            }    
            else cbElectrodeDefined.setSelected(false);
            cbElectrodeDefined.setEnabled(true);
            //cbElectrodeDefined.setSelected(ElectrodeDefined);
        }
        allowActionPerformed=true;
    }
    public void changeGrid() {
        World world=resultView.getWorld();
        SelectedGrid=cbGrids.getSelectedIndex();
        if (SelectedGrid>=GridList.size()) SelectedGrid=GridList.size()-1;
        System.out.println("SelectedGrid="+SelectedGrid);
        SelectedElectrode=-1;
        ElectrodeDefined=false;
        render.InteractiveObject.exitSubEdit(world);
        render.InteractiveObject.deselectAll(world);
        if (SelectedGrid>=0) {
            GridList.get(SelectedGrid).getGroupObject().setSelected(world, true);
        }
        render.InteractiveObject.enterSubEdit(world);
        updateGridAndElectrodeInterfaces();
        resultView.update();
        resultPlane.update();
    }
    public void gridObjectsChanged() {
        if (updateLock) return;
        updateLock=true;
        System.out.println("gridObjectsChanged");
        Grid g=null;
        SelectedGrid=-1;
        SelectedElectrode=-1;
        // Consider the first element as being the most important
        int i=0;
        while (i<GridList.size() && g==null) {
            int j=GridList.get(i).firstSelectedObject();
            if (j>=0) {
                SelectedGrid=i;
                SelectedElectrode=j;
                g=GridList.get(i);
            }
            else i++;
        }
        if (g==null && GridList.size()>0) {
            // Select first grid in GridList by default
            SelectedGrid=0;
            SelectedElectrode=0;
            g=GridList.get(0);
        }
        ElectrodeDefined=(SelectedElectrode>=0);
        /*
        System.out.println("SelectedGrid="+SelectedGrid);
        System.out.println("SelectedElectrode="+SelectedElectrode);
        System.out.println("ElectrodeDefined="+ElectrodeDefined);
        */
        updateGridAndElectrodeInterfaces();
        updateTextGraphics();
        updateLock=false;
    }
    public void updateTextGraphics() {
        World world=resultView.getWorld();
        if (world==null) return;
        world.removeGraphics3D();
        if (cbShowNumbers==null || !cbShowNumbers.isSelected()) return;
        Font f=new Font("TimesRoman", Font.PLAIN, 15);
        int dim[]=world.getBlockDimensions();
        double voxel[]=world.getVoxelDimensions();
        for(Grid g : GridList) {
            Vector<Electrode> el=g.getElectrodeList();
            for(int i=0;i<el.size();i++) {
                Electrode e=el.get(i);
                Vec v=e.getUserPos();if (v!=null) {
                    for(int j=0;j<3;j++) v.data[j]=v.data[j]/voxel[j]+dim[j]/2;
                    TextGraphic3D tg=new TextGraphic3D(""+(i+g.getFirstNumber()),f, v);
                    tg.setHideZ(10);
                    world.addGraphic3D(tg);
                }
                v=e.getPredictedPos();
                if (v!=null && e.getPredictedObject().getVisible()) {
                    for(int j=0;j<3;j++) v.data[j]=v.data[j]/voxel[j]+dim[j]/2;
                    TextGraphic3D tg=new TextGraphic3D(""+(i+g.getFirstNumber()),f, v);
                    tg.setHideZ(10);
                    world.addGraphic3D(tg);
                }
            }
        }
    }
    public void displayScreen(int screen) {
        desktop.global.MouseSelectsWindows=false;
        desktop.global.ManipAction=GlobalData.MANIP.SNAP_DATA;
        desktop.syncMenuWithGlobalData();
        desktop.global.DisplayImages.fireActionPerformed(this, "mouse_select");
        if (resultView==null) return;
        World world=resultView.getWorld();
        desktop.global.CurrentWorld=world;
        super.displayScreen(screen);
    }
    protected Date getValidationDate() {
        return utils.getDate(2021,4,7);
    }
    protected long getDaysThreshold() {
        return 365;
    }
}
