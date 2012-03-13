package com.rackspace.idm.documentation.samples;

public class SampleCreator {

	public static void main(String[] args) {
		try {
			if (args.length > 0 && args[0].equals("true")) {
				SampleFileWriter writer = new SampleFileWriter();
				writer.setFileOutputLocation("src/formattedsamples/keystone");
				
				//=======================================================//
				// 				LIST OF SAMPLES TO WRITE				 //
				//=======================================================//
				writer.writeSample(new ImpersonationRequest());
				writer.writeSample(new ImpersonationResponse());
		
				System.out.println("Finished generating samples");
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
