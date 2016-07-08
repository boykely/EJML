import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

public class Fonction implements LevenbergMarquardt.Function 
{	
	private Color lightColor;
	private int currentColor;
	public DenseMatrix64F oneChannelMData;	
	public int compte=0;
	public DenseMatrix64F Y;
	private HashMap<Integer, int[]>p;
	private int[] cam;
	//lafortune 
	private int[] lum;
	@Override
	public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y) 
	{
		// TODO Auto-generated method stub
		//y=a*x^2+b*x+c
		//A ne pas supprimer parce que utiliser pour des test simple
		/*for(int i=0;i<x.numRows;i++)
		{
			double xx=x.get(i, 0);
			y.set(i, 0, param.get(0, 0)*xx*xx+param.get(1,0)*xx+param.get(2,0));			
		}*/		
		double pixelX;
		byte[] pixelResult=new byte[3];		
		int[] pos=new int[3];
		double[] E=new double[3];
		double[] L=new double[3];
		double D2;			
		double[] H=new double[3];
		SimpleMatrix R;
		double[] Hn=new double[3];
		double[] Hnp=new double[3];
		SimpleMatrix S,M;
		double[] HnpW=new double[3];
		byte[] alb_d=new byte[3];
		byte[] alb_s=new byte[3];
		byte[] specv=new byte[]{1,1,(byte)200};
		//double alpha=1;//lobe speculaire => plus grand plus retrecie et concentré
		//double intensity=0.3;//varie 0 à 1
		double rod=param.get(0,0);
		double ros=param.get(1, 0);
		double s1=param.get(2,0);
		double s2=param.get(3,0);
		double s3=param.get(4,0);
		double nx=param.get(5,0);
		double ny=param.get(6,0);
		double nz=1;		
		double tof=param.get(7,0);		
		double alpha=param.get(8, 0);	
		double m=0.3;
		//contrainte paramètres
		rod=Math.abs(rod);
		ros=Math.abs(ros);
		alpha=Math.abs(alpha);
		s1=Math.abs(s1);
		s2=Math.abs(s2);
		s3=Math.abs(s3);
		/*
		rod=Math.exp(rod);
		ros=Math.exp(ros);
		s1+=0.001;
		s2+=0.001;
		double[] N=MappingN(nx, ny);		
		tof=Math.exp(tof)+0.3;
		alpha=Math.exp(alpha)+0.5;*/
		//double[] N=MappingN(nx, ny);
		try
		{			
			compte++;
			for(int i=0;i<x.numRows;i++)
			{
				double xx=x.get(i,0);
				pixelX=oneChannelMData.get(i, 0);//it will contain a pixel value. And it depends of the value of which channel
				pos=decodePosition(xx);
				//double[] newPos=normalize(pos);
				//E=normalize(cam-pos)
				E=normalize(XY(pos,cam));
				L=calculeL(pos,lum);
				D2=dot(E,E);
				double[] le=addXY(L,E);
				H=normalize(le);
				double[] N=normalize(ChangeBase(new double[]{nx,ny,2}));//on a changé N par Nx et Ny en tant que paramètre de L-M => step2_1104_soir
				double angle=Math.acos(Math.max(0,dot(N,H)))/m;
				double spec=1*Math.exp(-(angle*angle));
				double cosine=Math.max(0, dot(N,E));
				//sqrt is necessary to "rough gamma" => I don't understand it but if we omit the sqrt the colors are not correct
				double v=(((spec*ros)+(rod))*cosine/D2);//on va commenter tous les paramètres mapping => step2_1104_soir
				v=Math.sqrt(v);
				double value=v>256?255:v<0?0:v;
				y.set(i, 0,value);//intensity=0.5
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		Y=y;		
	}
	public Fonction(int x,int y,Color light,int c)
	{
		lightColor=light;
		currentColor=c;
		cam=ChangeBase(new int[]{0,0,1});
		lum=ChangeBase(new int[]{0,0,1});//=> fichier optimize2 avec une lissage sur tous les tiles avant de optimisation et optimize3 sans lissage
	}
	public void getPositionList(HashMap<Integer,int[]>pos)
	{
		p=pos;
	}
	public static int[] ChangeBase(int[] xyz)
	{
	        int[] P = new int[3];
	        int[] u = new int[] { 1, 0, 0 };
	        int[] v = new int[] { 0, 1, 0 };
	        int[] w = new int[] { 0, 0, 1 };
	        P[0] = u[0] * xyz[0]+(2304/2) ;
	        P[1] = v[1] * xyz[1] +(3264/2);
	        P[2] = w[2]*xyz[2];
	        return P;
	 }
	public static double[] ChangeBase(double[] xyz)
	{
		double[] P = new double[3];
		double[] u = new double[] { 1, 0, 0 };
		double[] v = new double[] { 0, 1, 0 };
		double[] w = new double[] { 0, 0, 1 };
		P[0] = u[0] * xyz[0]+(2304/2) ;
        P[1] = v[1] * xyz[1] +(3264/2);
        P[2] = w[2]*xyz[2];
        return P;
	}
	
	public static int[] decodePosition(double pos)
	{
		int[] p=new int[3];
		String[] pattern=Double.toString(pos).split("\\.");
		p[0]=Integer.parseInt(pattern[0]);
		p[1]=Integer.parseInt(pattern[1].substring(0, pattern[1].length()-1));
		p[2]=0;
		return p;
	}
	/*
	 * This is used for BRDF model A
	 */
	public static int[] XY(int[] x,int[] y)
	{
		return new int[]{y[0]-x[0],y[1]-x[1],y[2]-x[2]};
	}
	public static double[] normalize(int[] x)
	{
		double norm=Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
		if(norm==0)return new double[3];
		return new double[]{x[0]/norm,x[1]/norm,x[2]/norm};
	}
	public static double[] normalize(double[] x)
	{
		double norm=Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
		return new double[]{x[0]/norm,x[1]/norm,x[2]/norm};
	}
	public static double dot(double[] x,double[] y)
	{
		if(x.length==2)return x[0]*y[0]+x[1]*y[1];
		return x[0]*y[0]+x[1]*y[1]+x[2]*y[2];
	}
	public static double dot(int[] x,int[] y)
	{
		return x[0]*y[0]+x[1]*y[1]+x[2]*y[2];
	}
	public static double[] calculeL(int[]x,int[]y)
	{
		int[] xy=XY(x,y);
		return normalize(xy);
	}
	public static double[] addXY(double[]x,double[] y)
	{
		return new double[]{x[0]+y[0],x[1]+y[1],x[2]+y[2]};
	}
	public static double[] calculeHn(double[] h,double[] n,SimpleMatrix r)
	{		
		SimpleMatrix ha=new SimpleMatrix(new double[][]{{h[0]},{h[1]},{h[2]}});
		SimpleMatrix first=ha.plus(r.mult(ha)).plus(1);
		SimpleMatrix second=r.mult(r.mult(ha)).mult(new SimpleMatrix(new double[][]{{n[2]+1}}));
		return first.elementDiv(second).getMatrix().data;		
	}
	public static double[] div(double[] xy,double e)
	{
		return new double[]{xy[0]/e,xy[1]/e,xy[2]/e};
	}
	public static double[] calculeHnpW(SimpleMatrix m,double x,double y)
	{
		SimpleMatrix xy=new SimpleMatrix(new double[][]{
			{x},
			{y}
		});
		SimpleMatrix temp=m.mult(xy);
		double[] res=temp.getMatrix().data;
		return new double[]{res[0],res[1],1};
	}
	public static int byteColorCVtoIntJava(byte b)
	{		
		int i=(b+128)+128;		
		return b>=0?(int)b:i;
	}
	public static double[] MappingN(double nx,double ny)
	{
		return new double[]{
			nx/Math.sqrt((nx*nx+ny*ny)+1),
			ny/Math.sqrt((nx*nx+ny*ny)+1),
			1/Math.sqrt((nx*nx+ny*ny)+1)
		};
	}
	public static double v_p(double[] p,double tof)
	{		
		return Math.exp(-(p[0]*p[0]+p[1]*p[1])/(tof*tof));
	}
	/*
	 * This
	 * is used for Lafortune model
	 */
	/*
	private double v_p(int[] p,double tof)
	{
		double norm_p=normeX(p);
		return Math.exp(-norm_p/(tof*tof));
	}
	private double c_P(int[] n,int[] EP)
	{
		return Math.max(0, dot(n,EP));
	}
	public static int[] vectXY(int[] x,int[] y)
	{
		int[] xy=new int[3];
		xy[0]=y[0]-x[0];
		xy[1]=y[1]-x[1];
		xy[2]=y[2]-x[2];
		return xy;
	}
	
	public static double normeX(int[]x)
	{
		return x[0]*x[0]+x[1]*x[1]+x[2]*x[2];
	}
	private double dot(int[] x,int[] y)
	{
		return x[0]*y[0]+x[1]*y[1]+x[2]*y[2];
	}
	private double dot(double[] x,double[] y)
	{
		return x[0]*y[0]+x[1]*y[1]+x[2]*y[2];
	}
	public static double[] normalize(int[] x)
	{
		double mod=Math.sqrt(normeX(x));
		double[] y=new double[3];
		y[0]=((double)x[0])/mod;
		y[1]=((double)x[1])/mod;
		y[2]=((double)x[2])/mod;
		return y;
	}*/
}
