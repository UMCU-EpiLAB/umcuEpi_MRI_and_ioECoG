package utils;

import jni.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public class Vec {
    protected static boolean SHOW_WARNINGS = false;
    public double data[];
    
    public Vec() {
        set(0, 0, 0);
    }
    
    public Vec(double x, double y, double z) {
        set(x, y, z);
    }
    
    public Vec(double x, double y, double z, double t) {
        set(x, y, z, t);
    }
    
    public Vec(double[] p) {
        set(p);
    }
    public Vec(int dim) {
        if (dim >= 0) {
            setDim(dim);
        } else {
            System.err.println("Invalid dimension in Vec constructor.");
        }
    }
    public Vec(int[] p) {
        set(p);
    }
    public Vec(Vec p) {
        set(p);
    }
    public Vec(Vec p, double t) {
        // Convience function to make a 4D vector from a 3D vector
        set(p, t);
    }
    public Vec(String filename) {
        set(0, 0, 0);
        read(filename);
    }
    public void setDim(int dim) {
        if (dim >= 0) {
            data = new double[dim];
            for (int i = 0; i < dim; i++) data[i] = 0;
        } else {
            System.err.println("Invalid dimension in Vec.setDim.");
        }
    }
    public void changeDim(int dim, double defaultValue) {
        int oldLength = data.length;
        data = Arrays.copyOf(data, dim);
        for (int i = oldLength; i < dim; i++) data[i] = defaultValue;
    }
    public void changeDim(int dim) {
        data = Arrays.copyOf(data, dim);
    }
    public int getDim() {
        return data.length;
    }
    public void zero() {
        for (int i = 0; i < data.length; i++) data[i] = 0;
    }
    public void set(double x, double y, double z) {
        if (data == null || data.length != 3) data = new double[3];
            data[0] = x;
            data[1] = y;
            data[2] = z;
    }
    public void set(double x, double y, double z, double t) {
        if (data == null || data.length != 4) data = new double[4];
            data[0] = x;
            data[1] = y;
            data[2] = z;
            data[3] = t;
    }
    public void set(double[] p) {
        if (data == null || data.length != p.length) data = new double[p.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = p[i];
        }
    }
    public void set(int[] p) {
        if (data == null || data.length != p.length) data = new double[p.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = p[i];
        }
    }
    public void set(Vec p) {
    s   et(p.data);
    }
    public void set(Vec p, double t) {
        // Convience function to make a 4D vector from a 3D vector
        setDim(p.data.length + 1);
        // System.out.println("New length in Vec:set(p,t)="+data.length);
        for (int i = 0; i < p.data.length; i++) {
            data[i] = p.data[i];
        }
        data[data.length - 1] = t;
    }
    public void setX(double v) {
        data[0] = v;
    }
    public void setY(double v) {
        data[1] = v;
    }
    public void setZ(double v) {
        data[2] = v;
    }
    public void setT(double v) {
        data[3] = v;
    }
    public void setV(int i, double v) {
        if (i >= 0 && i < data.length) data[i] = v;
    }
    public double getX() {
        return getV(0);
    }
    public double getY() {
        return getV(1);
    }
    public double getZ() {
        return getV(2);
    }
    public double getT() {
        return getV(3);
    }
    public double getV(int i) {
        return (i >= 0 && i < data.length ? data[i] : 0);
    }
    public int[] get() {
        return utils.DoubleToIntVec(data);
    }
    public boolean isEqual(Vec v) throws VecException {
        Vec w = new Vec(v);
        w.sub(this, v);
        return (Math.abs(w.length()) < 1e-6 * length());
    }
    public String toStringFormat(String format) {
        String st = "|";
        for (int i = 0; i < data.length; i++) {
            // st+=" "+Sfp.printFormat("%8le", data[i])+" ";
            // st+=" "+Sfp.printFormat("%lg", data[i])+" ";
            st += " " + String.format(Locale.ENGLISH, format, data[i]);
        }
        return st + "|";
    }
    public String toString() {
        return toStringFormat("% 8e");
    }
    public void valueOf(String s) throws NumberFormatException {
        if (s == null) throw new NullPointerException();
        if (s.charAt(0) != '|' || s.charAt(s.length() - 1) != '|') throw new NumberFormatException();
        String sub = s.substring(2, s.length() - 2);
        sub = sub.trim();
        String sub1;
        Vector<Double> w = new Vector<Double>();
        int i, j;
        while (sub.length() > 0) {
            i = sub.indexOf(' ');
            j = sub.indexOf('\t');
            if ((i > j && j != -1) || i == -1) i = j;
            if (i == -1) i = sub.length();
            sub1 = sub.substring(0, i);
            w.add(Double.valueOf(sub1));
            sub = sub.substring(i, sub.length()).trim();
        }
        // Copy Vector elements to double array
        if (data.length != w.size()) data = new double[w.size()];
        for (i = 0; i < w.size(); i++) {
            data[i] = w.get(i).doubleValue();
        }
    }
    public void read(LineNumberReader lnr) {
        try {
            String line = lnr.readLine();
            int d = Integer.valueOf(line);
            if (d < 1) {
                System.err.println("Vec.read: Error reading size.");
                return;
            }
            setDim(d);
            line = lnr.readLine();double arr[] = utils.ReadVec(line);
            for (d = 0; d < data.length; d++) {
                data[d] = arr[d];
            }
        } 
        catch (IOException ioe) {
            System.err.println("Vec.read: Error parsing file");
        }
    }
    public void read(String filename) {
        if (filename == null) return;
        filename = utils.addCurrentDirectory(filename, false, new String[]{"txt"}, "Vector files", null);
        try {
            FileReader fr = new FileReader(filename);
            LineNumberReader lnr = new LineNumberReader(fr);
            read(lnr);
            fr.close();
        } 
        catch (IOException ioe) {
            System.err.println("IO exception when reading " + filename);
        }
    }
    public void save(PrintWriter pw) {
        pw.println(data.length);
        for (int d = 0; d < data.length; d++) {
            pw.format(Locale.ENGLISH, "% 8e ", data[d]);
        }
        pw.println();
    }
    public void save(String filename) {
        if (filename == null) return;
        filename = utils.addCurrentDirectory(filename, true,new String[]{"txt"}, "Vector files", null);
        try {
            FileWriter fw = new FileWriter(filename);
            PrintWriter pw = new PrintWriter(fw);
            save(pw);
            fw.close();
        } 
        catch (IOException ioe) {
            System.err.println("IO exception when writing " + filename);
        }
    }
    // Arithmetic operations
    public void add(Vec x, double v) throws VecException {
        if (data.length != x.data.length) throw new VecException("unequal vector sizes in Vec.add.");
        for (int i = 0; i < data.length; i++) {
            data[i] = x.data[i] + v;
        }
    }
    public void add(double v) throws VecException {
        add(this, v);
    }
    public void sub(Vec x, double v) throws VecException {
        if (data.length != x.data.length) throw new VecException("unequal vector sizes in Vec.sub.");
        for (int i = 0; i < data.length; i++) {
            data[i] = x.data[i] - v;
        }
    }
    public void sub(double v) throws VecException {
        sub(this, v);
    }
    public void add(Vec x, Vec y) throws VecException {
        int l = data.length;
        if (l != x.data.length || l != y.data.length) throw new VecException("Inequal lengths in Vec.add.");
        for (int e = 0; e < data.length; e++) {
            data[e] = x.data[e] + y.data[e];
        }
    }
    public void add(Vec x) throws VecException {
        add(this, x);
    }
    public void sub(Vec x, Vec y) throws VecException {
        int l = data.length;
        if (l != x.data.length || l != y.data.length) throw new VecException("Inequal lengths in Vec.sub.");
        for (int e = 0; e < data.length; e++) {
            data[e] = x.data[e] - y.data[e];
        }
    }
    public void sub(Vec x) throws VecException {
        sub(this, x);
    }
    public void max(Vec x, Vec y) throws VecException {
        int l = data.length;
        if (l != x.data.length || l != y.data.length) throw new VecException("Inequal lengths in Vec.max.");
        for (int e = 0; e < data.length; e++) {
            data[e] = Math.max(x.data[e],y.data[e]);
        }
    }
    public void max(Vec x) throws VecException {
        max(this, x);
    }
    public void min(Vec x, Vec y) throws VecException {
        int l = data.length;
        if (l != x.data.length || l != y.data.length) throw new VecException("Inequal lengths in Vec.min.");
        for (int e = 0; e < data.length; e++) {
            data[e] = Math.min(x.data[e],y.data[e]);
        }
    }
    public void min(Vec x) throws VecException {
        min(this, x);
    }
    public void dot(Vec x, double fac) throws VecException {
        if (data.length != x.data.length) throw new VecException("Inequal lengths in Vec.dot.");
        for (int e = 0; e < data.length; e++) {
            data[e] = x.data[e] * fac;
        }
    }
    public void dot(double fac) throws VecException {
        dot(this, fac);
    }
    public void div(Vec x, double fac) throws VecException {
        fac = 1. / fac;
        if (data.length != x.data.length) throw new VecException("unequal vector sizes in Vec.div.");
        for (int i = 0; i < data.length; i++) {
            data[i] = x.data[i] * fac;
        }
    }
    public void div(double fac) throws VecException {
        div(this, fac);
    }
    public double dot(Vec x) throws VecException {
        if (data.length != x.data.length) throw new VecException("unequal vector sizes in Vec.dot.");
        double sum = 0;
        for (int e = 0; e < data.length; e++) {
            sum += data[e] * x.data[e];
        }
        return sum;
        }
    public void dot(Vec x, Vec y) throws VecException {
        if ((data.length != x.data.length) || (data.length != y.data.length)) throw new VecException("Inequal lengths in Vec.dot.");
        for (int e = 0; e < data.length; e++) {
            data[e] = x.data[e] * y.data[e];
        }
    }
    public void cross(Vec x, Vec y) throws VecException {
        if (data.length < 3 || x.data.length < 3 || y.data.length < 3) throw new VecException("Vec.cross only implemented for 3D.");
        if (x == this || y == this) throw new VecException("Vec.cross needs different input " + "and output vectors.");
        /* this=x*y */
        data[0] = x.data[1] * y.data[2] - x.data[2] * y.data[1];
        data[1] = x.data[2] * y.data[0] - x.data[0] * y.data[2];
        data[2] = x.data[0] * y.data[1] - x.data[1] * y.data[0];
    }
    public double length() {
        try {
            return Math.sqrt(dot(this));
        } 
        catch (VecException ve) {
            // This is almost impossible..
            ve.printStackTrace();
            return -1;
        }
    }
    public double normalize() {
        double length = length();
        if (length > 0) {
            double l_length = 1 / length;
            for (int i = 0; i < data.length; i++) {
                data[i] *= l_length;
            }
        }
        return length;
    }
    public double getMax() {
        double max = 0;
        for (int i = 0; i < data.length; i++) {
            if (i == 0 || data[i] > max) max = data[i];
        }
        return max;
    }
    public double getMin() {
        double min = 0;
        for (int i = 0; i < data.length; i++) {
            if (i == 0 || data[i] < min) min = data[i];
        }
        return min;
    }
    public void dot(Mat m, Vec v) throws VecException {
        if (m.ncols != v.data.length || m.nrows != data.length) throw new VecException("Number of columns differs from number" + " of vector elements.");
        if (this == v) throw new VecException("Input and output vector " + "should be different in Vec.dot.");
        for (int row = 0; row < m.nrows; row++) {
            data[row] = 0;
            for (int col = 0; col < m.ncols; col++) {
                data[row] += m.data[row * m.ncols + col] * v.data[col];
            }
        }
    }
    public boolean intersectLineWithSurface(Vec PosLine,Vec DirLine,Vec PosSurface, Vec NormalSurface, double lambda[]) throws VecException {
        Vec TempVec = new Vec(NormalSurface);
        TempVec.setT(0);
        double denom = TempVec.dot(DirLine);
        if (denom == 0) return false;
        TempVec.sub(PosSurface, PosLine);
        TempVec.setT(0);
        lambda[0] = NormalSurface.dot(TempVec) / denom;
        TempVec.dot(DirLine, lambda[0]);
        add(PosLine, TempVec);
        return true;
    }
}
