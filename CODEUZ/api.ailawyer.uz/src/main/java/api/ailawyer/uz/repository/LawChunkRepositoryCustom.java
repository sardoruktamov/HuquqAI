package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawChunkEntity;

import java.util.List;

public interface LawChunkRepositoryCustom {

    List<LawChunkEntity> findSimilarChunks(float[] queryVector, int topK);
}
