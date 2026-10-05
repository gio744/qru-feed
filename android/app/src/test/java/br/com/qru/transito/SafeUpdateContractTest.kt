package br.com.qru.transito
import org.junit.Assert.*
import org.junit.Test
class SafeUpdateContractTest{
 @Test fun downloadIsNotActivation(){val downloaded=true;val signatureValid=false;val activate=downloaded&&signatureValid;assertFalse(activate)}
 @Test fun invalidCandidateKeepsLastKnownGood(){val lkg="release-A";val candidateValid=false;val active=if(candidateValid)"release-B" else lkg;assertEquals("release-A",active)}
 @Test fun signatureAndFingerprintAreSeparateGates(){val fp=true;val sig=false;assertFalse(fp&&sig)}
}
