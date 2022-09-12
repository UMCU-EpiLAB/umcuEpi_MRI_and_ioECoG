package utils;
import jni.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public class Mat {
    protected int nrows=0, ncols=0;
    public double data[]=null;
    public Mat() {
        setDim(3,3);
    }
    public Mat(Mat p) {
        set(p);
    }
    public Mat(int nr, int nc) {
        setDim(nr, nc);
    }
    public Mat(String filename) {
        setDim(3,3);
        read(filename);
    }
    public int getNRows() {
        return nrows;
    }
    public int getNCols() {
        return ncols;
    }
    public void setDim(int nr, int nc) {
        if (nr<=0 || nc<=0) {
            System.err.println("Invalid dimensions in Mat.setDim.");
            return;
        }
        if (nrows!=nr || ncols!=nc) {
            nrows=nr;
            ncols=nc;
            data=new double[size()];
        }
        set(0.);
    }
    public void changeDim(int nr, int nc) {
        // Try to keep contents while changing dimensions
        if (nr <= 0 || nc <= 0) {
            System.err.println("Invalid dimensions in Mat.changeDim.");
            return;
        }
        if (nrows == nr && ncols == nc) return;
        double data_new[] = new double[nr * nc];
        for (int i = 0; i < nr * nc; i++) data_new[i] = 0;
        for (int r = 0; r < nr && r < nrows; r++) {
            for (int c = 0; c < nc && c < ncols; c++) {
                data_new[r * nc + c] = data[r * ncols + c];
            }
        }
        nrows = nr;
        ncols = nc;
        data = data_new;
    }
    public void set(Mat m) {
        if (m==this) return;
        setDim(m.nrows, m.ncols);
        for(int i=0; i<size(); i++) {
            data[i]=m.data[i];
        }
    }
    public void setRow(int row, Vec v) throws MatException {
        if (v.data.length!=ncols) throw new MatException("vector size unappropriate for Mat.setRow.");
        for(int col=0; col<ncols; col++) {
            data[row*ncols+col]=v.data[col];
        }
    }
    public void getRow(int row, Vec v) throws MatException {
        if (v.data.length!=ncols) throw new MatException("vector size unappropriate for Mat.getRow.");
        for(int col=0; col<ncols; col++) {
            v.data[col]=data[row*ncols+col];
        }
    }
    public void setCol(int col, Vec v) throws MatException {
        if (v.data.length!=nrows) throw new MatException("vector size unappropriate for Mat.setCol.");
        for(int row=0; row<nrows; row++) {
            data[row*ncols+col]=v.data[row];
        }
    }
    public void getCol(int col, Vec v) throws MatException {
        if (v.data.length!=nrows) throw new MatException("vector size unappropriate for Mat.getCol.");
        for(int row=0; row<nrows; row++) {
            v.data[row]=data[row*ncols+col];
        }
    }
    public void set(double v) {
        for(int i=0; i<size(); i++) {
            data[i]=v;
        }
    }
    public void set(int row, int col, double v) {
        data[row*ncols+col]=v;
    }
    public double get(int row, int col) {
        return data[row*ncols+col];
    }
    public void clear() {
        set(0.);
    }
    public void unity() {
        int count=nrows;
        if (count>ncols) count=ncols;
        clear();
        for(int i=0; i<count; i++) data[i*ncols+i]=1.;
    }
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.0000000");
        String st="";
        for(int row=0; row<nrows; row++) {
            st+="|";
            for(int col=0; col<ncols; col++) {
                // st+=" "+Sfp.printFormat("%8le",data[row*ncols+col])+" ";
                st+=" "+String.format(Locale.ENGLISH, "% 8e", data[row*ncols+col]);
            }
            st+="|\n";
        }
        st+="\n";
        return st;
    }
    int size() {
        return nrows*ncols;
    }
    boolean equalDims(Mat m) {
        return (nrows==m.nrows && ncols==m.ncols);
    }
    public boolean read(LineNumberReader lnr) {
        try {
            String line=lnr.readLine();
            System.out.println("#"+line+"#"+line.length());
            int sizes[]=utils.decodeSize(line);
            if (sizes[0]<1 || sizes[1]<1) {
                System.err.println("Mat.read: Error reading sizes.");
                return false;
            }
            setDim(sizes[0], sizes[1]);
            for(int r=0;r<nrows;r++) {
                line=lnr.readLine();
                double arr[]=utils.ReadVec(line);
                for(int c=0;c<ncols;c++) {
                    set(r,c,arr[c]);
                }
            }
        }
        catch (IOException ioe) {
            System.err.println("Mat.read: Error parsing file");
            return false;
        }
        return true;
    }
    public boolean read(String filename) {
        if (filename==null) return false;
        filename=utils.addCurrentDirectory(filename, false,
        new String[]{"txt"}, "Matrix files", null);
        System.out.println("Trying to read "+filename);
        try {
            FileReader fr=new FileReader(filename);
            LineNumberReader lnr=new LineNumberReader(fr);
            if (!read(lnr)) return false;
            fr.close();
        }
        catch (IOException ioe) {
            System.err.println("IO exception when reading "+filename);
            return false;
        }
        return true;
    }
    public void save(PrintWriter pw) {
        pw.println(nrows+"x"+ncols);
        for(int row=0; row<nrows; row++) {
            for(int col=0; col<ncols; col++) {
                pw.format(Locale.ENGLISH, "% 8e ", data[row*ncols+col]);
            }
            pw.println();
        }
    }
    public boolean save(String filename) {
        if (filename==null) return false;
        filename=utils.addCurrentDirectory(filename, true, new String[]{"txt"}, "Matrix files", null);
        try {
            FileWriter fw=new FileWriter(filename);
            PrintWriter pw=new PrintWriter(fw);
            save(pw);
            fw.close();
        }
        catch (IOException ioe) {
            System.err.println("IO exception when writing "+filename);
            return false;
        }
        return true;
    }
    // Arithmetic operations
    public void add(Mat m1, Mat m2) throws MatException {
        if (!m1.equalDims(m2)) throw new MatException("Inequal dimensions in Mat.add.");
        changeDim(m1.getNRows(), m1.getNCols());
        for(int i=0; i<size(); i++) {
            data[i]=m1.data[i]+m2.data[i];
        }
    }
    public void add(Mat m) throws MatException {
        add(this, m);
    }
    public void sub(Mat m1, Mat m2) throws MatException {
        if (!m1.equalDims(m2)) throw new MatException("Inequal dimensions in Mat.sub.");
        changeDim(m1.getNRows(), m1.getNCols());
        for(int i=0; i<size(); i++) {
            data[i]=m1.data[i]-m2.data[i];
        }
    }
    public void sub(Mat m) throws MatException {
        sub(this, m);
    }
    public void dot(Mat m, double fac) throws MatException {
        changeDim(m.getNRows(), m.getNCols());
        for(int i=0; i<size(); i++) {
            data[i]=fac*m.data[i];
        }
    }
    public void dot(double fac) {
        try {
            dot(this, fac);
        }
        catch (MatException me) {
            // It should be impossible to come here..
            me.printStackTrace();
        }
    }
    public void dot(Mat m1, Mat m2) throws MatException {
        if (this==m1 || this==m2) throw new MatException("Resulting matrix should be different "+ "from input matrices.");
        if (m1.ncols!=m2.nrows) throw new MatException("Invalid dimensions of matrices in Mat.dot");
        changeDim(m1.nrows, m2.ncols);
        for(int row=0; row<nrows; row++) {
            for(int col=0; col<ncols; col++) {
                double s=0;
                for(int i=0; i<m1.ncols; i++) {
                    s+=m1.data[row*m1.ncols+i]*m2.data[i*m2.ncols+col];
                }
                data[row*ncols+col]=s;
            }
        }
    }
    public void initRotMatrix(double Angle, int ConstDir) {
        changeDim(4,4);int RotDir1, RotDir2; // the rotation direction that are affected
        // Initialize all elements to zeroes
        clear();
        double c=Math.cos(Angle);
        double s=Math.sin(Angle);
        double fac=1;
        switch (ConstDir) {
            case 0:
            default:
            RotDir1=1;
            RotDir2=2;
            break;
            case 1:
            RotDir1=0;
            RotDir2=2;
            fac=-1;
            break;
            case 2:
            RotDir1=0;
            RotDir2=1;
            break;
        }
        set(RotDir1,RotDir1,c);
        set(RotDir1,RotDir2,-s*fac);
        set(RotDir2,RotDir1,s*fac);
        set(RotDir2,RotDir2,c);
        set(ConstDir,ConstDir,1.0);
        set(3,3,1.0);
        // To keep translation ability
    }
    public void initThreeAxisRotMatrix(double angles[]) {
        /* first rotate around x-axis, then around y-axis, */
        /* and at last around z-axis */
        try {
            Mat temp1=new Mat(4,4);
            Mat temp2=new Mat(4,4);
            initRotMatrix(angles[0], 0);
            //System.out.println("rotx, angle="+angles[0]+":\n"+this);
            temp1.initRotMatrix(angles[1], 1);
            //System.out.println("roty, angle="+angles[1]+":\n"+temp1);
            temp2.dot(temp1, this);
            //System.out.println("temp2:\n"+temp2);
            temp1.initRotMatrix(angles[2], 2);
            //System.out.println("rotz, angle="+angles[2]+":\n"+temp1);
            dot(temp1, temp2);
        }
        catch (MatException me) {
            // We should never come here..
            me.printStackTrace();
            return;
        }
    }
    public void initThreeAxisRotMatrixAndTranslation(double angles[], double trans[]) {
        initThreeAxisRotMatrix(angles);
        set(0,3,trans[0]);
        set(1,3,trans[1]);
        set(2,3,trans[2]);
    }
    private double myatan(double v) {
        if (v>=Double.MAX_VALUE) return Math.PI/2.;
        if (v<=-Double.MAX_VALUE) return -Math.PI/2.;
        return Math.atan(v);
    }
    private double myasin(double v) {
        if (v>=1) return Math.PI/2.;
        if (v<=-1) return -Math.PI/2.;
        return Math.asin(v);
    }
    private double myacos(double v) {
        if (v>=1) return 0;
        if (v<=-1) return Math.PI;
        return Math.acos(v);
    }
    public void getAnglesThreeAxisRotMatrix(double angles[]) {  
        /* calculate the angles for an three axis rotation matrix */
        /* WARNING: row and column vectors must be normalized */
        if (nrows < 3 || ncols < 3) {
            System.err.println("Minimal 3D matrices required in
            Mat.getAnglesThreeAxisRotMatrix");
            return;
        }
        if (nrows != ncols) {
            System.err.println("Not a square matrix in Mat.getAnglesThreeAxisRotMatrix");
            return;
        }
        if (angles.length < (nrows - 1)) {
            System.err.println("Arrays not big enough in
            Mat.getAnglesThreeAxisRotMatrix");
            return;
        }
        // Algorithm from Computing Euler angles from a rotation matrix
        // Gregory G. Slabaugh
        // https://www.gregslabaugh.net/publications/euler.pdf
        double R11=get(0,0);
        double R12=get(0,1);
        double R13=get(0,2);
        double R21=get(1,0);
        double R31=get(2,0);
        double R32=get(2,1);
        double R33=get(2,2);
        double theta, psi, phi;
        double theta1, theta2;
        double psi1, psi2;
        double phi1, phi2;
      
        if ((R31>-1+1e-6) && (R31<1-1e-6)) {
            //System.out.println("hier1, R31="+R31);
            theta1=-myasin(R31);
            theta2=Math.PI-theta1;
            psi1=Math.atan2(R32/Math.cos(theta1), R33/Math.cos(theta1));
            psi2=Math.atan2(R32/Math.cos(theta2), R33/Math.cos(theta2));
            phi1=Math.atan2(R21/Math.cos(theta1), R11/Math.cos(theta1));
            phi2=Math.atan2(R21/Math.cos(theta2), R11/Math.cos(theta2));
            // Choose solution with angles closest to zero
            double sum1=Math.abs(theta1)+Math.abs(psi1)+Math.abs(phi1);
            double sum2=Math.abs(theta2)+Math.abs(psi2)+Math.abs(phi2);
            if (sum1<sum2) {
                theta=theta1;
                psi=psi1;
                phi=phi1;
            }
            else {
                theta=theta2;
                psi=psi2;
                phi=phi2;
            }
        }
        else {
            phi=0;
            if (R31<=-1+1e-6) {
                //System.out.println("hier2");
                theta=Math.PI/2;
                psi=phi+Math.atan2(R12, R13);
            }
            else {
                //System.out.println("hier3");
                theta=-Math.PI/2;
                psi=-phi+Math.atan2(-R12, -R13);
            }
        }
        while (psi<-Math.PI) psi+=2*Math.PI;
        while (psi>Math.PI) psi-=2*Math.PI;
        while (theta<-Math.PI) theta+=2*Math.PI;
        while (theta>Math.PI) theta-=2*Math.PI;
        while (phi<-Math.PI) phi+=2*Math.PI;
        while (phi>Math.PI) phi-=2*Math.PI;
        angles[0] = psi;
        angles[1] = theta;
        angles[2] = phi;
    }
    public void getAnglesAndTranslationThreeAxisRotMatrix(double angles[], double trans[]) {
        if (nrows<3 || ncols<3) {
            System.err.println("Minimal 3D matrices required in
            Mat.getAnglesAndTranslationThreeAxisRotMatrix");
            return;
        }
        if (nrows!=ncols) {
            System.err.println("Not a square matrix in
            Mat.getAnglesAndTranslationThreeAxisRotMatrix");
            return;
        }
        if (angles.length<(nrows-1) || trans.length<(nrows-1)) {
            System.err.println("Arrays not big enough in
            Mat.getAnglesAndTranslationThreeAxisRotMatrix");
            return;
        }
        Mat m=new Mat(this);
        Vec v=new Vec(nrows);
        try {
            getCol(3,v);
            for(int i=0;i<3;i++) {
                trans[i]=v.getV(i);
                m.set(i,3,0);
            }
            m.getAnglesThreeAxisRotMatrix(angles);
        }
        catch (MatException me) {
            me.printStackTrace();
            return;
        }
    }
    public void trans(Mat m) throws MatException {
        if (this==m) throw new MatException("Input and output matrices should be "+ "different in Mat.trans.");
        changeDim(m.getNCols(), m.getNRows());
        for(int r=0; r<nrows; r++) {
            for(int c=0; c<ncols; c++) {
                set(r,c,m.get(c,r));
            }
        }
    }
    public boolean inverse(Mat m) throws MatException {
        if (m.nrows!=m.ncols) throw new MatException("Inequal dimensions in Mat.inverse.");
        set(m);
        int N=getNRows();
        double d[]=new double[1];
        Vec col=new Vec(N);
        int indx[]=new int[N];
        Mat TempMat=new Mat(m);
        boolean result=ludcmp(TempMat,indx,d);
        if (result) {
            for(int j=0; j<N; j++) {
                for(int i=0; i<N; i++) col.data[i]=0.0;
                col.data[j]=1.0;
                lubksb(TempMat,indx,col);
                setCol(j,col);
            }
        }
        return result;
    }
    public boolean inverse() {
        try {
            return inverse(this);
        }
        catch (MatException me) {
            // We should never come here..
            me.printStackTrace();
            return false;
        }
    }
    public double determinant() throws MatException {
        if (getNRows()!=getNCols()) throw new MatException("Inequal dimensions in Mat.determinant");
        Mat A=new Mat(this);
        int N=getNRows();
        for(int j=0; j<N; j++) {
            double max=0.0;
            for(int i=0; i<N; i++) {
                double val=Math.abs(A.get(i,j));
                if (val>max) max=val;
            }
            if (max==0.0) {
                return 0; /* singular matrix */
            }
        }
        int indx[]=new int[N];
        double d[]=new double[1];
        ludcmp( A, indx, d);
        for(int i=0;i<N;i++) d[0]*=A.get(i,i);
        return d[0];
    }
    public boolean same(Mat m) {
        if (nrows != m.nrows || ncols != m.ncols) return false;
        boolean same1 = true;
        for (int r = 0; r < nrows; r++) {
            for (int c = 0; c < ncols; c++) {
                if (Math.abs(get(r, c) - m.get(r, c)) > 0.001) {
                    System.out.println("diff at "+r+", "+c);
                    same1 = false;
                }
            }
        }
        return same1;
    }
    private boolean ludcmp(Mat M, int[] indx, double[] d) throws MatException {
        /**
        * Given a NxN matrix M[0..N-1][0..N-1], this routine replaces it by the LU
        * decomposition of a rowwise permutation of itself. M is input. M is output,
        * arranged as in equation (2.3.14) of Numerical Recipes; indx[0..N-1] is an
        * output vector which records the row permutation effected by the partial
        * pivoting; d is output as +_1 depending on whether the number of row
        * interchanges was even or odd, respectively. This routine is used in
        * combination
        * with lubksb to solve linear equations or invert a matrix.
        * ( Numerical Recipes Reprint 1990)
        */
        int N=getNRows();
        int i,imax=-1,j,k;
        double big,dum,sum,temp;
        double vv[]=new double[N];
        d[0]=1.0;
        for (i=0; i<N; i++) {
            big=0.0;
            for (j=0; j<N; j++) {
                if ((temp=Math.abs(M.get(i,j))) > big) big=temp;
            }    
            if (big == 0.0) {
                System.out.println("M="+M);
                throw new MatException("Singular matrix in routine LUDCMP");
            }
            vv[i]=1.0/big;
        }
        for (j=0; j<N; j++) {
            for (i=0; i<j; i++) {
                sum=M.get(i,j);
                for (k=0; k<i; k++) sum -= M.get(i,k)*M.get(k,j);
                M.set(i,j,sum);
            }
            big=0.0;
            for (i=j; i<N; i++) {
                sum=M.get(i,j);
                for (k=0; k<j; k++) sum -= M.get(i,k)*M.get(k,j);
                M.set(i,j,sum);
                if ( (dum=vv[i]*Math.abs(sum)) >= big) {
                    big=dum;
                    imax=i;
                }
            }
            if (j != imax) {
                for (k=0; k<N; k++) {
                    dum=M.get(imax,k);
                    M.set(imax,k,M.get(j,k));
                    M.set(j,k,dum);
                }
                d[0] = -(d[0]);
                vv[imax]=vv[j];
            }
            indx[j]=imax;
            if (Math.abs(M.get(j,j))<Double.MIN_VALUE) M.set(j,j,Double.MIN_VALUE);
            if (j != N-1) {
                dum=1.0/(M.get(j,j));
                for (i=j+1; i<N; i++) M.set(i,j,M.get(i,j)*dum);
                if (i==100) {
                    System.err.println("dum="+dum); // We should never come here
                }
            }
        }
        return true;
    }
    private void lubksb(Mat M, int[]indx, Vec b) {
        /**
        * Solves the set of 4 linear equations A.x = b. Here M[0..N-1][0..N-1] is input,
        * not as the matrix A but rather as its LU decomposition, determined by the
        * routine ludcmp. indx[0..N-1] is input as the permutation vector returned by
        * ludcmp. b[0..N-1] is input as the right-hand side vector B, and returns with
        * the solution vector X. M and indx are not modified by this routine and can be
        * left in place for successive calls with different right-hand sides b.
        * This routine takes into account the possibility that b will begin with
        * many zero elements, so it is efficient for use in matrix inversion.
        */
        int i,ii=-1,ip,j;
        double sum;
        int N=getNRows();
        for (i=0; i<N; i++) {
            ip=indx[i];
            sum=b.data[ip];
            b.data[ip]=b.data[i];
            if (ii>=0) for (j=ii; j<=i-1; j++) sum -= M.get(i,j)*b.data[j];
            else if (sum!=0.) ii=i;
            b.data[i]=sum;
        }
        for (i=N-1; i>=0; i--) {
            sum=b.data[i];
            for (j=i+1; j<N; j++) sum -= M.get(i,j)*b.data[j];
            b.data[i]=sum/M.get(i,i);
        }
    }
    private double pythag(double a, double b) {
        double at=Math.abs(a);
        double bt=Math.abs(b);
        double ct;
        if (at>bt) {
            ct=bt/at;
            return at*Math.sqrt(1+ct*ct);
        }
        else {
            if (bt!=0) {
                ct=at/bt;
                return bt*Math.sqrt(1+ct*ct);
            }
            else return 0;
        }
    }
    private double sign(double a, double b) {
        a=Math.abs(a);
        return (b>=0? a: -a);
    }
    /*public int varCounter=0;
    public void dumpVars(int i,
    double[][] a, double[][] v, double[] rv1, double[] w) {
    System.out.println("dumpVars#"+(varCounter++)+", i="+i);
    System.out.println("a="+utils.WriteMat(a));
    System.out.println("v="+utils.WriteMat(v));
    System.out.println("rv1="+utils.WriteVec(rv1));
    System.out.println("w="+utils.WriteVec(w));
    }
    */
    public void pseudoInverse(Mat m) throws MatException {
        if (m.getNCols()>m.getNRows()) {
            // return pseudoInverse of transpose
            Mat mT=new Mat();
            mT.trans(m);
            pseudoInverse(mT);
            return;
        }
        set(m);
        Mat v=new Mat();
        Vec w=new Vec();
        svdcmp(w, v); // Matrix u comes into this
        double max=Math.abs(w.getV(0));
        for(int i=1;i<w.getDim();i++) if (max<Math.abs(w.getV(i))) max=Math.abs(w.getV(i));
        int n=m.getNRows();
        if (n>m.getNCols()) n=m.getNCols();
        double tol=n*max*1e-16;
        System.out.println("tol="+tol);
        int rank=0;
        Mat diag=new Mat(w.getDim(), w.getDim());
        diag.unity();
        for(int i=0;i<w.getDim();i++) {
            if (w.getV(i)>tol) {
                rank++;
                diag.set(i, i, 1./w.getV(i));
            }
            else diag.set(i, i, 0.);
        }
        System.out.println("rank="+rank);
        System.out.println("w="+w);
        System.out.println("diag="+diag);
        Mat mT=new Mat();
        mT.trans(this);
        Mat mTemp=new Mat();
        mTemp.dot(v, diag);
        dot(mTemp, mT); // Now this should contain pseudo inverse
    }
    public void svdcmp(Vec vec, Mat mat) {
        //varCounter=0;
        boolean flag;
        int i,its,j,jj,k,l=0,nm=0;
        double anorm=0,c,f,g=0,h,s,scale=0,x,y,z;
        double a[][]=new double[nrows+1][ncols+1];
        double v[][]=new double[nrows+1][ncols+1];
        for(i=0;i<nrows+1;i++) {
            Arrays.fill(a[i], 0);
            Arrays.fill(v[i], 0);
        }
        int m=nrows;
        int n=ncols;
        for(k=1; k<=m; k++) {
            for(i=1; i<=n; i++) {
                a[k][i]=get(k-1,i-1);
            }
        }
        double rv1[]=new double[n+1];
        double w[]=new double[n+1];
        Arrays.fill(rv1, 0);
        Arrays.fill(w, 0);
        for (i=1;i<=n;i++) {
            l=i+1;
            rv1[i]=scale*g;
            g=s=scale=0.0;
            if (i <= m) {
                for (k=i;k<=m;k++) scale += Math.abs(a[k][i]);
                if (scale!=0) {
                    for (k=i;k<=m;k++) {
                        a[k][i] /= scale;
                        s += a[k][i]*a[k][i];
                    }
                    f=a[i][i];
                    g = -sign(Math.sqrt(s),f);
                    h=f*g-s;
                    a[i][i]=f-g;
                    for (j=l;j<=n;j++) {
                        for (s=0.0,k=i;k<=m;k++) s += a[k][i]*a[k][j];
                        f=s/h;
                        for (k=i;k<=m;k++) a[k][j] += f*a[k][i];
                    }
                    for (k=i;k<=m;k++) a[k][i] *= scale;
                }
            }
            w[i]=scale *g;
            g=s=scale=0.0;
            if (i <= m && i != n) {
                for (k=l;k<=n;k++) scale += Math.abs(a[i][k]);
                if (scale!=0) {
                    for (k=l;k<=n;k++) {
                        a[i][k] /= scale;
                        s += a[i][k]*a[i][k];
                    }
                    f=a[i][l];
                    g = -sign(Math.sqrt(s),f);
                    h=f*g-s;
                    a[i][l]=f-g;
                    for (k=l;k<=n;k++) rv1[k]=a[i][k]/h;
                    for (j=l;j<=m;j++) {
                        for (s=0.0,k=l;k<=n;k++) s += a[j][k]*a[i][k];
                        for (k=l;k<=n;k++) a[j][k] += s*rv1[k];
                    }
                    for (k=l;k<=n;k++) a[i][k] *= scale;
                }
            }
            anorm = Math.max(anorm,(Math.abs(w[i])+Math.abs(rv1[i])));
        }
        for (i=n;i>=1;i--) { 
            /* Accumulation of right-hand transformations. */
            if (i < n) {
                if (g!=0) {
                    for (j=l;j<=n;j++) {
                        /* Double division to avoid possible underflow. */
                        v[j][i]=(a[i][j]/a[i][l])/g;
                    }
                    for (j=l;j<=n;j++) {
                        for (s=0.0,k=l;k<=n;k++) s += a[i][k]*v[k][j];
                        for (k=l;k<=n;k++) v[k][j] += s*v[k][i];
                    }
                }
                for (j=l;j<=n;j++) v[i][j]=v[j][i]=0.0;
            }
            v[i][i]=1.0;
            g=rv1[i];
            l=i;
        }
        for (i=Math.min(m, n);i>=1;i--) { /* Accumulation of left-hand transformations. */
            l=i+1;
            g=w[i];
            for (j=l;j<=n;j++) a[i][j]=0.0;
            if (g!=0) {
                g=1.0/g;
                for (j=l;j<=n;j++) {
                    for (s=0.0,k=l;k<=m;k++) s += a[k][i]*a[k][j];
                    f=(s/a[i][i])*g;
                    for (k=i;k<=m;k++) a[k][j] += f*a[k][i];
                }
                for (j=i;j<=m;j++) a[j][i] *= g;
            } else for (j=i;j<=m;j++) a[j][i]=0.0;
            ++a[i][i];
        }
        for (k=n;k>=1;k--) { /* Diagonalization of the bidiagonal form. */
            for (its=1;its<=30;its++) {
                flag=true;
                for (l=k;l>=1;l--) { /* Test for splitting. */
                    nm=l-1; /* Note that rv1[1] is always zero. */
                    if ((double)(Math.abs(rv1[l])+anorm) == anorm) {
                        flag=false;break;
                    }
                    if ((double)(Math.abs(w[nm])+anorm) == anorm) break;
                }
                if (flag) {
                    c=0.0; /* Cancellation of rv1[l], if l > 1. */
                    s=1.0;
                    for (i=l;i<=k;i++) {
                        f=s*rv1[i];
                        rv1[i]=c*rv1[i];
                        if ((double)(Math.abs(f)+anorm) == anorm) break;
                        g=w[i];
                        h=pythag(f,g);
                        w[i]=h;
                        h=1.0/h;
                        c=g*h;
                        s = -f*h;
                        for (j=1;j<=m;j++) {
                            y=a[j][nm];
                            z=a[j][i];
                            a[j][nm]=y*c+z*s;
                            a[j][i]=z*c-y*s;
                        }
                    }
                }
                z=w[k];
                if (l == k) { /* Convergence. */
                    if (z < 0.0) { /* Singular value is made nonnegative. */
                        w[k] = -z;
                        for (j=1;j<=n;j++) v[j][k] = -v[j][k];
                    }
                    break;
                }
                if (its == 30) System.out.println("no convergence in 30 svdcmp iterations");
                x=w[l]; /* Shift from bottom 2-by-2 minor. */
                nm=k-1;
                y=w[nm];
                g=rv1[nm];
                h=rv1[k];
                f=((y-z)*(y+z)+(g-h)*(g+h))/(2.0*h*y);
                g=pythag(f,1.0);
                f=((x-z)*(x+z)+h*((y/(f+sign(g,f)))-h))/x;
                c=s=1.0; /* Next QR transformation: */
                for (j=l;j<=nm;j++) {
                    i=j+1;
                    g=rv1[i];
                    y=w[i];
                    h=s*g;
                    g=c*g;
                    z=pythag(f,h);
                    rv1[j]=z;
                    c=f/z;
                    s=h/z;
                    f=x*c+g*s;
                    g = g*c-x*s;
                    h=y*s;
                    y *= c;
                    for (jj=1;jj<=n;jj++) {
                        x=v[jj][j];
                        z=v[jj][i];
                        v[jj][j]=x*c+z*s;
                        v[jj][i]=z*c-x*s;
                    }
                    z=pythag(f,h);
                    w[j]=z; /* Rotation can be arbitrary if z = 0. */
                    if (z!=0) {
                        z=1.0/z;
                        c=f*z;
                        s=h*z;
                    }
                    f=c*g+s*y;
                    x=c*y-s*g;
                    for (jj=1;jj<=m;jj++) {
                        y=a[jj][j];
                        z=a[jj][i];
                        a[jj][j]=y*c+z*s;
                        a[jj][i]=z*c-y*s;
                    }
                }
                rv1[l]=0.0;rv1[k]=f;
                w[k]=x;
            }
        }
        mat.changeDim(m, n);
        vec.changeDim(n);
        for(k=1; k<=m; k++) {
            for(i=1; i<=n; i++) {
                set(k-1,i-1, a[k][i]);
                mat.set(k-1,i-1, v[k][i]);
            }
        }
        for(k=1; k<=n; k++) {
            vec.setV(k-1, w[k]);
        }
    }
    public void vXvT(Vec v, Vec w) {
        changeDim(v.getDim(), w.getDim());
        for(int x=0;x<v.getDim();x++) {
            for(int y=0;y<w.getDim();y++) {
                set(x,y, v.data[x]*w.data[y]);
            }
        }
    }
}
