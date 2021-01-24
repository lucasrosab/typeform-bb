package portal.autenticador;

/**
 *
 * @author f9575449
 */
public class UsuarioSSO {

    public String tokenId;
    public String acessos;
    public String chave;
    public String email;
    public String nome;
    public String telefone;
    public String endereco;
    public String codigoComissao;
    public String cpf;
    public String codigoPilar;
    public String nomeUF;
    public String prefixoDependencia;
    public String descricaoComissao;
    public String tipoDependencia;
    public String prefixoSuperEstadual;
    public String uorDependencia;
    public String uorTrabalho;
    public String prefixoDiretoria;
    public String nomeGuerra;
    public String displayName;
    public String rf;
    public String nivelCargo;
    public String grupamento;
    public String codigoInstituicao;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getAcessos() {
        return acessos;
    }

    public void setAcessos(String acessos) {
        this.acessos = acessos;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCodigoComissao() {
        return codigoComissao;
    }

    public void setCodigoComissao(String codigoComissao) {
        this.codigoComissao = codigoComissao;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCodigoPilar() {
        return codigoPilar;
    }

    public void setCodigoPilar(String codigoPilar) {
        this.codigoPilar = codigoPilar;
    }

    public String getNomeUF() {
        return nomeUF;
    }

    public void setNomeUF(String nomeUF) {
        this.nomeUF = nomeUF;
    }

    public String getPrefixoDependencia() {
        return prefixoDependencia;
    }

    public void setPrefixoDependencia(String prefixoDependencia) {
        this.prefixoDependencia = prefixoDependencia;
    }

    public String getDescricaoComissao() {
        return descricaoComissao;
    }

    public void setDescricaoComissao(String descricaoComissao) {
        this.descricaoComissao = descricaoComissao;
    }

    public String getTipoDependencia() {
        return tipoDependencia;
    }

    public void setTipoDependencia(String tipoDependencia) {
        this.tipoDependencia = tipoDependencia;
    }

    public String getPrefixoSuperEstadual() {
        return prefixoSuperEstadual;
    }

    public void setPrefixoSuperEstadual(String prefixoSuperEstadual) {
        this.prefixoSuperEstadual = prefixoSuperEstadual;
    }

    public String getUorDependencia() {
        return uorDependencia;
    }

    public void setUorDependencia(String uorDependencia) {
        this.uorDependencia = uorDependencia;
    }

    public String getUorTrabalho() {
        return uorTrabalho;
    }

    public void setUorTrabalho(String uorTrabalho) {
        this.uorTrabalho = uorTrabalho;
    }

    public String getPrefixoDiretoria() {
        return prefixoDiretoria;
    }

    public void setPrefixoDiretoria(String prefixoDiretoria) {
        this.prefixoDiretoria = prefixoDiretoria;
    }

    public String getNomeGuerra() {
        return nomeGuerra;
    }

    public void setNomeGuerra(String nomeGuerra) {
        this.nomeGuerra = nomeGuerra;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRf() {
        return rf;
    }

    public void setRf(String rf) {
        this.rf = rf;
    }

    public String getNivelCargo() {
        return nivelCargo;
    }

    public void setNivelCargo(String nivelCargo) {
        this.nivelCargo = nivelCargo;
    }

    public String getGrupamento() {
        return grupamento;
    }

    public void setGrupamento(String grupamento) {
        this.grupamento = grupamento;
    }

    public String getCodigoInstituicao() {
        return codigoInstituicao;
    }

    public void setCodigoInstituicao(String codigoInstituicao) {
        this.codigoInstituicao = codigoInstituicao;
    }
    
    public boolean getTemAcesso(String acesso) {
        int pos = acessos.indexOf(acesso);
        return pos > 0;
    }
}
