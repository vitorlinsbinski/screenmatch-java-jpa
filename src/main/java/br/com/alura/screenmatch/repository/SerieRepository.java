package br.com.alura.screenmatch.repository;

import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface SerieRepository extends JpaRepository<Serie, Long> {
    // Derived queries
    Optional<Serie> findByTituloContainingIgnoreCase(String nomeSerie);

    List<Serie> findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(String nomeAtor, Double avaliacao);

    List<Serie> findTop5ByOrderByAvaliacaoDesc();

    List<Serie> findByGenero(Categoria categoria);

    List<Serie> findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(Integer totalTemporadas, Double avaliacao);

    @Query("select s from Serie s where s.totalTemporadas <= :totalTemporadas" +
            " and s.avaliacao >=" +
            " :avaliacao order by s.avaliacao desc")
    List<Serie> seriesPorTemporadaEAvaliacao(Integer totalTemporadas, Double avaliacao);

    @Query("select e from Episodio e where e.titulo ilike %:trecho%")
    List<Episodio> buscarEpisodiosPorTrecho(String trecho);

    @Query("select e from Episodio e order by e.avaliacao desc limit 5")
    List<Episodio> top5Episodios();

    @Query("select e from Episodio e where e.serie = :serie " +
            "order by e.avaliacao desc limit 5")
    List<Episodio> top5EpisodiosPorSerie(Serie serie);

    @Query("select e from Episodio e where e.serie = :serie and year(e" +
            ".dataLancamento) >= :ano")
    List<Episodio> episodiosPorSerieDepoisDeUmaData(Serie serie,
                                                    Integer ano);
}
