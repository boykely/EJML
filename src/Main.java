import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.ejml.data.DenseMatrix64F;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Main 
{	
	public static void main(String[] args)
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try
		{
			FileWriter writerB=new FileWriter("C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\testB.txt");
			FileWriter writerG=new FileWriter("C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\testG.txt");
			FileWriter writerR=new FileWriter("C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\testR.txt");
			String dir="C:\\Users\\ralambomahay1\\Downloads\\stage\\twoshot_data_input\\book_leather_red\\";//"C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\";
			String path="source_tile_relit_";
			//BufferedImage[][] tiles=new BufferedImage[12][16];
			int tileWNumber=17;
			int tileHNumber=12;
			int brdfEstimation=tileWNumber*tileHNumber;
			BufferedImage[][] tiles=new BufferedImage[tileHNumber][tileWNumber];
				
			Color color;
			int c,b,g,r;			
			
			List<double[]> blueData=new ArrayList<>();
			List<double[]> greenData=new ArrayList<>();
			List<double[]> redData=new ArrayList<>();	
			HashMap<Integer, int[]> p=new HashMap<>();
			double[][] posData=new double[192*192][];//it is used only for test			
			List<DenseMatrix64F> posDataMatrix=new ArrayList<>();//it contains 192*192 DenseMatrix64F. Each item refers to position array in world coordinates
			List<DenseMatrix64F> paramBList=new ArrayList<>();//it contains all parameters for each pixel
			List<DenseMatrix64F> paramGList=new ArrayList<>();//it contains all parameters for each pixel
			List<DenseMatrix64F> paramRList=new ArrayList<>();//it contains all parameters for each pixel
			
			int colonne=192;
			int ligne=192;
			int pI=0;
			List<DenseMatrix64F> blueMData=new ArrayList<>();			
			List<DenseMatrix64F> greenMData=new ArrayList<>();
			List<DenseMatrix64F> redMData=new ArrayList<>();
			List<DenseMatrix64F> blueMDataAdjusted=new ArrayList<>();
			List<DenseMatrix64F> greenMDataAdjusted=new ArrayList<>();
			List<DenseMatrix64F> redMDataAdjusted=new ArrayList<>();
			
			System.out.println("start intialisation des données...");
			Mat tempMat;
			BufferedImage tempBuffer;
			for(int fileI=0;fileI<tileHNumber;fileI++)
			{							
				for(int fileJ=0;fileJ<tileWNumber;fileJ++)
				{									
					tempBuffer=ImageIO.read(new File(dir+path+fileI+"_"+fileJ+".jpg"));
					
					//on va lisser chaque tiles avant d'appliquer le svbrdf optimisation
					tempMat=convertTileToCV(tempBuffer);
					Imgproc.GaussianBlur(tempMat, tempMat, new Size(3,3), 1);
					tempBuffer=convertCVtoJava(tempMat);
					tiles[fileI][fileJ]=tempBuffer;
				}						
			}
			System.out.println("Fin lecture files");
			for(int i=0;i<192;i++)
			{
				for(int j=0;j<192;j++)
				{
					double[] blue=new double[brdfEstimation];
					double[] green=new double[brdfEstimation];
					double[] red=new double[brdfEstimation];
					double[] temp=new double[brdfEstimation];
					DenseMatrix64F tempPos=new DenseMatrix64F(brdfEstimation,1);
					DenseMatrix64F tempB=new DenseMatrix64F(brdfEstimation,1);
					DenseMatrix64F tempG=new DenseMatrix64F(brdfEstimation,1);
					DenseMatrix64F tempR=new DenseMatrix64F(brdfEstimation,1);
					int iter=0;
					for(int tileI=0;tileI<tileHNumber;tileI++)
					{						
						for(int tileJ=0;tileJ<tileWNumber;tileJ++)
						{
							c=tiles[tileI][tileJ].getRGB(j, i);
							color=new Color(c);
							b=color.getBlue();
							g=color.getGreen();
							r=color.getRed();
							blue[iter]=b;
							green[iter]=g;
							red[iter]=r;
							//stocke position
							String pp=((tileI*ligne)+i)+"."+((tileJ*colonne)+j+"1");
							temp[iter]=Double.parseDouble(pp);
							iter++;			
						}
					}
					posData[pI]=temp;
					tempPos.data=temp;
					posDataMatrix.add(tempPos);
					pI++;
					blueData.add(blue);
					greenData.add(green);
					redData.add(red);
					tempB.data=blue;					
					tempG.data=green;
					tempR.data=red;
					blueMData.add(tempB);
					greenMData.add(tempG);
					redMData.add(tempR);					
				}
			}
			//String[] kk=Double.toString(posDataMatrix.get(0).get(1)).split("\\.");
			//System.err.println(kk[1].substring(0, kk[1].length()-1));
			//for(int i=0;i<192;i++)System.err.println(posDataMatrix.get(0).get(i)+" -- "+posData[0][i]);
			//for(int i=0;i<192;i++)System.out.println(blueMData.get(0).get(i, 0)+" -- "+greenMData.get(0).get(i, 0)+" -- "+redMData.get(0).get(i, 0));
			
			
			//
			System.out.println("fin initialisation des données.");
			//System.out.println(posData.length+"/"+greenMData.size()+"/"+redMData.size());
			Date dt=new Date();
			String now=dt.toString();
			System.out.println("Debut d'optimisation pour chaque pixel de chaque tile.. "+now);
			//init parameters value for all optimization
			Color av=AverageColor(dir+"flash.png");
			/*//Lafortune model optimization parameters
			DenseMatrix64F paramB=new DenseMatrix64F(new double[][]{				
				{((double)av.getBlue())/255},
				{1},
				{1},
				{3}
			});
			DenseMatrix64F paramG=new DenseMatrix64F(new double[][]{
				{((double)av.getGreen())/255},//rod				
				{1},
				{1},
				{3}
			});
			DenseMatrix64F paramR=new DenseMatrix64F(new double[][]{
				{((double)av.getRed())/255},//rod				
				{1},
				{1},
				{3}
			});*/
			DenseMatrix64F paramB=new DenseMatrix64F(new double[][]{
				{av.getBlue()},//rod
				{-1},//ros
				{-3},//s1
				{-3},//s2
				{0},//s3
				{10},//nx
				{10},//ny => nz = 1 
				{-1},//tof
				{0.4},//alpha				
			});
			DenseMatrix64F paramG=new DenseMatrix64F(new double[][]{
				{av.getGreen()},//rod
				{0},//ros
				{-3},//s1
				{-3},//s2
				{0},//s3
				{10},//nx
				{10},//ny => nz = 1 
				{-1},//tof
				{0.4},//alpha	
			});
			DenseMatrix64F paramR=new DenseMatrix64F(new double[][]{
				{av.getBlue()},//rod
				{0},//ros
				{-3},//s1
				{-3},//s2
				{0},//s3
				{10},//nx
				{10},//ny => nz = 1 
				{-1},//tof
				{0.4},//alpha	
			});
			try
			{
				DenseMatrix64F[] temp=new DenseMatrix64F[3];
				DenseMatrix64F[] Y=new DenseMatrix64F[3];
				for(int i=0;i<36864;i++)
				{
					//il faut optimiser chaque pixel
					Fonction f=new Fonction(17*192,12*192,av);
					Fonction f2=new Fonction(17*192,12*192,av);
					Fonction f3=new Fonction(17*192,12*192,av);
					f.oneChannelMData=blueMData.get(i);//f.lightColor=0;//optimize10 file
					f2.oneChannelMData=greenMData.get(i);//f2.lightColor=255;
					f3.oneChannelMData=redMData.get(i);//f3.lightColor=0;
					LevenbergMarquardt lm=new LevenbergMarquardt(f);
					LevenbergMarquardt lm2=new LevenbergMarquardt(f2);
					LevenbergMarquardt lm3=new LevenbergMarquardt(f3);
					DenseMatrix64F Yb=blueMData.get(i);
					DenseMatrix64F Yg=greenMData.get(i);
					DenseMatrix64F Yr=redMData.get(i);	
					//optimize5 (0,0,50);optimize6 (0,0,255)
					//blue
					lm.optimize(paramB, posDataMatrix.get(i), Yb);
					paramBList.add(lm.getParameters());
					//System.err.println(f.compte);f.compte=0;
					//green								
					lm2.optimize(paramG, posDataMatrix.get(i), Yg);
					paramGList.add(lm2.getParameters());
					//System.err.println(f.compte);f.compte=0;
					//Red											
					lm3.optimize(paramR, posDataMatrix.get(i), Yr);
					paramRList.add(lm3.getParameters());
					//System.err.println(f.compte);f.compte=0;
					if(i==0 || i==10000 || i==20000 ||i==30000)System.out.println("pixel:"+i+" optimisé");	
					//lm.getParameters().print();
					//lm2.getParameters().print();
					//lm3.getParameters().print();
					if(!Double.isNaN(lm.getParameters().get(0)))
					{
						temp[0]=new DenseMatrix64F(lm.getParameters());
						Y[0]=new DenseMatrix64F(f.Y);
					}
					if(!Double.isNaN(lm2.getParameters().get(0)))
					{
						temp[1]=new DenseMatrix64F(lm2.getParameters());
						Y[1]=new DenseMatrix64F(f2.Y);
					}
					if(!Double.isNaN(lm3.getParameters().get(0)))
					{
						temp[2]=new DenseMatrix64F(lm3.getParameters());
						Y[2]=new DenseMatrix64F(f3.Y);
					}
					WriteLogParameters(writerB, temp[0]);
					WriteLogParameters(writerG, temp[1]);
					WriteLogParameters(writerR, temp[2]);
					blueMDataAdjusted.add(Y[0]);
					greenMDataAdjusted.add(Y[1]);
					redMDataAdjusted.add(Y[2]);
				}
			}			
			catch(Exception ee)
			{
				System.err.println("Boucle optimize erreur!"+ee.getMessage());
			}
			writerB.close();writerG.close();writerR.close();
			dt=new Date();
			System.out.println("Fin de l'optimisation."+now+" à "+dt.toString());
			System.err.println("Display:");
			//for(int i=0;i<192;i++)System.out.println(blueMData.get(0).get(i, 0)+" -- "+blueMDataAdjusted.get(0).get(i)+"/"+greenMData.get(0).get(i, 0)+" -- "+greenMDataAdjusted.get(0).get(i)+"/"+redMData.get(0).get(i, 0)+" -- "+redMDataAdjusted.get(0).get(i));
			
			
			 //On peut faire la reconstruction dans cette partie ou optimisation
			BufferedImage image=new BufferedImage(tileWNumber*192,tileHNumber*192,BufferedImage.TYPE_3BYTE_BGR);
			int[] position;
			DenseMatrix64F temp;
			DenseMatrix64F tempR;
			DenseMatrix64F tempG;
			DenseMatrix64F tempB;
			
			for(int i=0;i<192*192;i++)
			{
				temp=posDataMatrix.get(i);
				tempR=redMDataAdjusted.get(i);
				//tempR=redMData.get(i);
				tempG=greenMDataAdjusted.get(i);
				//tempG=greenMData.get(i);
				tempB=blueMDataAdjusted.get(i);
				//tempB=blueMData.get(i);
				//paramB=paramBList.get(i);
				//paramG=paramGList.get(i);
				//paramR=paramRList.get(i);
				
				for(int j=0;j<brdfEstimation;j++)
				{
					position=Fonction.decodePosition(temp.get(j,0));
					//si <0 => valeur absoulue
					double rr=Math.abs(tempR.get(j, 0));
					double gg=Math.abs(tempG.get(j, 0));
					double bb=Math.abs(tempB.get(j, 0));
					//resultat fichier optimize4
					/*r=(rr>256)?255:(int)rr;
					g=(gg>256)?255:(int)gg;
					b=(bb>256)?255:(int)bb;*/
					r=(tempR.get(j, 0)>256)?255:( tempR.get(j,0)<0?0: (int)tempR.get(j,0));
					g=(tempG.get(j, 0)>256)?255:( tempG.get(j,0)<0?0: (int)tempG.get(j,0));
					b=(tempB.get(j, 0)>256)?255:( tempB.get(j,0)<0?0: (int)tempB.get(j,0));
					/*//recompute brdf
					int[] EP=Fonction.vectXY(Fonction.ChangeBase(new int[]{0,0,1}), position);
					double[] EPn=Fonction.normalize(EP);				
					int[] LP=Fonction.vectXY(Fonction.ChangeBase(new int[]{100,-50,1}),position);
					double[] LPn=Fonction.normalize(LP);				
					double lobe=paramB.get(1, 0)*(EPn[0]*LPn[0]+EPn[1]*EPn[1])+paramB.get(2,0)*(EPn[2]*LPn[2]);
					b=(int) (paramB.get(0,0)*tempB.get(j, 0)+Math.pow(lobe<0?0:lobe, paramB.get(3,0))*tempB.get(j, 0));	
					lobe=paramG.get(1, 0)*(EPn[0]*LPn[0]+EPn[1]*EPn[1])+paramG.get(2,0)*(EPn[2]*LPn[2]);
					g=(int) (paramG.get(0,0)*tempG.get(j, 0)+Math.pow(lobe<0?0:lobe, paramG.get(3,0))*tempG.get(j, 0));
					lobe=paramR.get(1, 0)*(EPn[0]*LPn[0]+EPn[1]*EPn[1])+paramR.get(2,0)*(EPn[2]*LPn[2]);
					r=(int) (paramR.get(0,0)*tempR.get(j, 0)+Math.pow(lobe<0?0:lobe, paramR.get(3,0))*tempR.get(j, 0));
					r=r>256?255:r;
					g=g>256?255:g;					
					b=b>256?255:b;*/
					color=new Color(r,g,b);					
					image.setRGB(position[1],position[0], color.getRGB());
				}
			}
			System.out.println("Debut enregistremen");
			saveImage(image, "C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\new_step2.jpg");
		}
		catch(Exception e)
		{
			System.err.println("Erreur general:"+e.getMessage());
		}		
		/*Fonction fonc=new Fonction();
		LevenbergMarquardt lm=new LevenbergMarquardt(fonc);
		//Seed inital parameters
		lm.optimize(new DenseMatrix64F(new double[][]{{0},{0},{0}}), new DenseMatrix64F(new double[][]{{0.2059},{0.4412},{0.7353},{0.9874},{1.1975}}), new DenseMatrix64F(new double[][]{{-1.0966},{0.4916},{1.4076},{1.0630},{0.2059}}));		
		DenseMatrix64F f=lm.getParameters();
		f.print();		
		fonc.Y.print();*/
	}
	public static int byteColorCVtoIntJava(byte b)
	{		
		int i=(b+128)+128;		
		return b>=0?(int)b:i;
	}
	public static double Gaussian(Random rd,double mu,double std)
	{
		//variance est déjà au carré			
		return rd.nextGaussian()*std+mu;
	}
	public static Color AverageColor(String path)
	{
		Color c=null;
		int temp;
		int tempR=0;
		int tempG=0;
		int tempB=0;
		try 
		{
			BufferedImage image=ImageIO.read(new File(path));
			//BufferedImage image=ImageIO.read(new File("C:\\Users\\ralambomahay1\\Downloads\\Java_workspace\\newGit\\Data\\book_black_input_flash.jpg"));
			for(int i=0;i<image.getHeight();i++)
			{
				for(int j=0;j<image.getWidth();j++)
				{
					temp=image.getRGB(j, i);
					c=new Color(temp);
					tempR+=c.getRed();
					tempG+=c.getGreen();
					tempB+=c.getBlue();
				}
			}
			tempR/=image.getHeight()*image.getWidth();
			tempG/=image.getHeight()*image.getWidth();
			tempB/=image.getHeight()*image.getWidth();
			c=new Color(tempR,tempG,tempB);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			System.out.println("AverageColor erreur:"+e.getMessage());
		}
		return c;
	}
	public static void saveImage(BufferedImage image,String path)
	{
		try
		{			
			//save image		
			ImageIO.write(image, "jpg", new File(path));			
		}
		catch(IOException e)
		{
			System.err.println("Erreur lors de l'enregistrement:"+e.getMessage());
		}		
	}
	public static Mat convertTileToCV(BufferedImage im)
	{
		Mat m=new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC3);
		byte[] data=((DataBufferByte)im.getRaster().getDataBuffer()).getData();
		m.put(0, 0, data);
		return m;
	}
	public static BufferedImage convertCVtoJava(Mat mat)
	{
		BufferedImage image=new BufferedImage(mat.cols(), mat.rows(),BufferedImage.TYPE_3BYTE_BGR);
		byte[] data=new byte[mat.rows()*mat.cols()*mat.channels()];
		mat.get(0, 0,data);
		byte[] dest=((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		System.arraycopy(data, 0, dest, 0, mat.rows()*mat.cols()*mat.channels());		
		return image;
	}
	public static void WriteLogParameters(FileWriter writer,DenseMatrix64F data)
	{
		try
		{
			double[] d=data.data;
			String temp="";
			for(int i=0;i<d.length;i++)
			{
				temp+=d[i]+"//";
			}
			writer.write(temp+"\n");
		}
		catch(IOException e)
		{
			System.err.println("Ecriture paramèretres fichiers erruers");
		}
	}
}
