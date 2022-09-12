package interactivegrid2;

import utils.Vec;
import utils.utils;
import render.*;

public class Electrode {
    private Primitive userObject=null, predictedObject=null;
    protected int xy[]=new int[2]; // Indices in array
    
    public Electrode(render.World w, int x, int y) {
        xy[0]=x;
        xy[1]=y;
        userObject=new render.Primitive(w, render.Primitive.TYPE.ELLIPSOID);
        // Set size to 2
        double param[]=userObject.getGeomParam();
        param[6]=2;
        param[7]=2;
        param[8]=2;
        userObject.setGeomParam(param);
        // User object is purple
        param=userObject.getColorParam();
        param[0]=1;
        param[1]=0;
        param[2]=1;
        userObject.setColorParam(param);
        
        predictedObject=new render.Primitive(w,render.Primitive.TYPE.ELLIPSOID);
        // Set size to 2
        param=predictedObject.getGeomParam();
        param[6]=2;
        param[7]=2;
        param[8]=2;
        predictedObject.setGeomParam(param);
        // Predicted object is cyan
        param=predictedObject.getColorParam();
        param[0]=0;
        param[1]=1;
        param[2]=1;
        predictedObject.setColorParam(param);
        
        userObject.setVisible(false);
        predictedObject.setVisible(false);
    }
    public void setName(String s) {
        userObject.setName(s+" user");
        predictedObject.setName(s+" predicted");
    }
    public void setUserPos(Vec v) {
        if (v!=null && v.getDim()>=3) {
            double param[]=userObject.getGeomParam();
            param[0]=v.getX();
            param[1]=v.getY();
            param[2]=v.getZ();
            userObject.setGeomParam(param);
            userObject.setVisible(true);
        }
        else userObject.setVisible(false);
        }
    public Vec getUserPos() {
        if (userObject.getVisible()) {
            double param[]=userObject.getGeomParam();
            return new Vec(param[0], param[1], param[2]);
        }
    else return null;
    }
    public void setPredictedPos(Vec v) {
        if (v!=null && v.getDim()>=3) {
            double param[]=predictedObject.getGeomParam();
            param[0]=v.getX();
            param[1]=v.getY();
            param[2]=v.getZ();predictedObject.setGeomParam(param);
            predictedObject.setVisible(true);
        }
        else predictedObject.setVisible(false);
        }
    public Vec getPredictedPos() {
        if (predictedObject.getVisible()) {
            double param[]=predictedObject.getGeomParam();
            return new Vec(param[0], param[1], param[2]);
        }
        else return null;
    }
    public Primitive getUserObject() {
        return userObject;
    }
    public Primitive getPredictedObject() {
        return predictedObject;
    }
}
