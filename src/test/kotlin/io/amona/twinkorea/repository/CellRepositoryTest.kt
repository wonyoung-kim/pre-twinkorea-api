package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Cell
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest
class TodoRepositoryTest{
    @Test
    fun findOneTest(repo: CellRepository){
        val cell: Optional<Cell> = repo.findById(3)
        println(cell)
    }
}