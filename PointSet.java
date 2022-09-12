package utils;
import java.util.*;

public class PointSet extends Vector<Vec> {
    public PointSet() {
    }
    public PointSet(PointSet ps) {
        init(ps);
    }
    void init(PointSet ps) {
        clear();
        for(int i=0;i<ps.size();i++) {
            add(new Vec(ps.get(i)));
        }
    }
    public Vec getV(int i) {
        return super.get(i);
    }
    public String toString() {
        String s="";
        for(int i=0;i<size();i++) {
            s+=get(i)+System.lineSeparator();
        }
        return s;
    }
    public Mat calcRigidTransformation(PointSet ps) throws VecException, MatException {
        int size=size();
        if (size<3 || size!=ps.size()) {
            System.err.println("Insufficient number of points in
            PointSet.calcRigidTransformation");
            return null;
        }
        int dim=get(0).getDim();
        // System.out.println("calculate mean");
        Vec mean1=new Vec(dim);
        Vec mean2=new Vec(dim);
        for(int i=0;i<size;i++) {
            mean1.add(getV(i));
            mean2.add(ps.getV(i));
        }
        mean1.div(size);
        mean2.div(size);
        // System.out.println("mean1="+mean1);
        // System.out.println("mean2="+mean2);
        // System.out.println("subtract mean");
        PointSet q1=new PointSet(this);
        PointSet q2=new PointSet(ps);// Subtract mean
        for(int i=0;i<size;i++) {
            q1.get(i).sub(mean1);
            q2.get(i).sub(mean2);
        }
        // Add a little bit of noise as svdcmp fails when trying to find perfect matches
        for(int i=0;i<size;i++) {
            Vec vq1=q1.get(i);
            Vec vq2=q2.get(i);
            for(int j=0;j<3;j++) {
                vq1.setV(j, vq1.getV(j)*(1+Math.random()/10000000000.));
                vq2.setV(j, vq2.getV(j)*(1+Math.random()/10000000000.));
            }
        }
        System.out.println("q1="+q1);
        System.out.println("q2="+q2);
        // System.out.println("calculate H");
        // Calculate H
        Mat H=new Mat(dim, dim);
        for(int i=0;i<size;i++) {
            Mat H1=new Mat(dim, dim);
            H1.vXvT(q1.getV(i), q2.getV(i));
            H.add(H1);
        }
        H.dot(1./size);
        System.out.println("H="+H);
        // System.out.println("calling svdcmp");
        Vec L=new Vec(dim);
        Mat V=new Mat(dim, dim);
        H.svdcmp(L, V);
        // System.out.println("L="+L);
        // System.out.println("V="+V);
        System.out.println("H="+H);
        Mat HT=new Mat(dim, dim);
        HT.trans(H);
        // System.out.println("calculate X");
        Mat X=new Mat(dim, dim);
        X.dot(V, HT);
        if (X.determinant()<0) {
            Vec lastVec=new Vec(dim);
            V.getCol(dim-1, lastVec);
            lastVec.dot(-1);
            V.setCol(dim-1, lastVec);
            X.dot(V, HT);
        }
        // System.out.println("X="+X);
        Vec T=new Vec(mean1);
        T.dot(X, mean1);
        T.sub(mean2,T);
        System.out.println("T="+T);
        // System.out.println("calculate Y");
        Mat Y=new Mat(dim+1, dim+1);
        for(int r=0;r<dim;r++) {
            for(int c=0;c<dim;c++) {
                Y.set(r,c,X.get(r,c));
            }
            Y.set(r,3,T.data[r]);
        }
        Y.set(3,3,1);
        return Y;
    }
    public void transform(Mat m, PointSet ps) {
        init(ps);
        try {
            int nCols=m.getNCols();
            int nRows=m.getNRows();
            Vec v=new Vec(nCols);
            Vec w=new Vec(nRows);
            
            for(int i=0;i<ps.size();i++) {
                for(int r=0;r<nCols;r++) {
                    v.setV(r, ps.get(i).getV(r));
                }
                v.setT(1);
                w.dot(m, v);
                System.out.println("w="+w);
                for(int r=0;r<get(i).getDim();r++) {
                    get(i).setV(r,w.getV(r));
                }
            }
        }
        catch (VecException ve) {
            System.out.println("PointSet.transform VecException"+ve);
        }
    }
}
