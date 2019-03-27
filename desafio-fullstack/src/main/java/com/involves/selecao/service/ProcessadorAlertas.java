package com.involves.selecao.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonToken;
import com.involves.selecao.alerta.Alerta;
import com.involves.selecao.alerta.Pesquisa;
import com.involves.selecao.alerta.Resposta;
import com.involves.selecao.gateway.AlertaGateway;

@Service
public class ProcessadorAlertas {

	@Autowired
	private AlertaGateway gateway;

	public void processa() throws IOException {
		URL url = new URL("http://selecao-involves.agilepromoter.com/pesquisas");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
		String inputLine;
		StringBuffer content = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();

		Gson gson = new Gson();
		System.out.println(content.toString());
		Pesquisa[] ps = gson.fromJson(content.toString(), Pesquisa[].class);
		// Pesquisa[] ps = gson.fromJson(content.toString(), Pesquisa[].class);

		List<Pesquisa> pesquisas = new ArrayList<Pesquisa>();
		for (int i = 0; i < ps.length; i++) {
			pesquisas.add(ps[i]);
		}		
		
		pesquisas.forEach(pesquisa -> {
			List<Resposta> respostas = pesquisa.getRespostas();

			respostas.forEach(resposta -> {
				if (resposta.getPergunta().equals("Qual a situação do produto?")) {
					if (resposta.getResposta().equals("Produto ausente na gondola")) {
						Alerta alerta = new Alerta();
						alerta.setPontoDeVenda(pesquisa.getPonto_de_venda());
						alerta.setDescricao("Ruptura detectada!");
						alerta.setProduto(pesquisa.getProduto());
						alerta.setFlTipo(1);
						gateway.salvar(alerta);
					}
				} else if (resposta.getPergunta().equals("Qual o preço do produto?")) {
					int precoColetado = Integer.parseInt(resposta.getResposta());
					int precoEstipulado = Integer.parseInt(pesquisa.getPreco_estipulado());
					if (precoColetado > precoEstipulado) {
						Alerta alerta = new Alerta();
						int margem = precoEstipulado - Integer.parseInt(resposta.getResposta());
						alerta.setMargem(margem);
						alerta.setDescricao("Preço acima do estipulado!");
						alerta.setProduto(pesquisa.getProduto());
						alerta.setPontoDeVenda(pesquisa.getPonto_de_venda());
						alerta.setFlTipo(2);
						gateway.salvar(alerta);
					} else if (precoColetado < precoEstipulado) {
						Alerta alerta = new Alerta();
						int margem = precoEstipulado - Integer.parseInt(resposta.getResposta());
						alerta.setMargem(margem);
						alerta.setDescricao("Preço abaixo do estipulado!");
						alerta.setProduto(pesquisa.getProduto());
						alerta.setPontoDeVenda(pesquisa.getPonto_de_venda());
						alerta.setFlTipo(3);
						gateway.salvar(alerta);
					}
				} else {
					System.out.println("Alerta ainda não implementado!");
				}
			});
		});

	}
}
