import unittest
from audit_reference_identity import inspect, norm_article

class IdentityAuditTests(unittest.TestCase):
    def candidate(self,code='551-71',article='181, XIV',source=''):
        return {'package': {'items': [{'stable_key':'a','code':code,'article':article,'source':source}]}}
    def test_cross_reference_does_not_verify_fiche(self):
        rows=inspect(self.candidate(),'Quando NÃO autuar: utilizar 551-71, Art. 181, XIV.','')
        self.assertFalse(rows[0]['identity_found_in_supplied_sources'])
    def test_wrong_inciso_does_not_match(self):
        text='Tipificação Resumida: Estacionar 551-71\nAmparo Legal:\nArt. 181, XV.\nTipificação do Enquadramento:'
        self.assertFalse(inspect(self.candidate(),text,'')[0]['identity_found_in_supplied_sources'])
    def test_manual_header_and_pdf_page(self):
        text='\fTipificação Resumida: Estacionar 551-71\nAmparo Legal:\nArt. 181, XIV.\nTipificação do Enquadramento:'
        row=inspect(self.candidate(source='https://www.gov.br/x/mbvt20222.pdf#page=2'),text,'')[0]
        self.assertTrue(row['source_page_matches_header'])
        self.assertFalse(row['operational_ready'])
    def test_other_pdf_anchor_is_not_compared_to_manual(self):
        text='Tipificação Resumida: Estacionar 551-71\nAmparo Legal:\nArt. 181, XIV.\nTipificação do Enquadramento:'
        row=inspect(self.candidate(source='https://www.gov.br/x/resolucao.pdf#page=3'),text,'')[0]
        self.assertIsNone(row['source_page_matches_header'])
    def test_split_table_code(self):
        row=inspect(self.candidate(),'','5517   1   Estacionar nos viadutos    181, XIV    Condutor')[0]
        self.assertTrue(row['identity_found_in_supplied_sources'])
    def test_article_suffix_is_preserved(self):
        self.assertNotEqual(norm_article('165-A'),norm_article('165'))
        self.assertNotEqual(norm_article('165-B, caput'),norm_article('165-B, parágrafo único'))

if __name__=='__main__':unittest.main()
