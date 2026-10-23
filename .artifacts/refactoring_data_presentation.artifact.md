# Refatoração da Camada de Dados e Apresentação (Fase 1)

Esta é a estrutura base para substituirmos o `SharedPreferences` e o `MainViewModel` por **Room**, **Hilt**, **Coroutines Flow** e **UiState**.

## 1. As Entidades de Banco de Dados (Room)
Separamos a lógica visual (`BankEntity` atual) do banco de dados relacional físico.

```kotlin
@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val presetKey: String,
    val defaultDueDay: Int,
    val closingDay: Int,
    val creditLimit: Double,
    val isArchived: Boolean
)

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Bank::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE // Se deletar o banco, deleta as faturas junto
        )
    ],
    indices = [Index("bankId")]
)
data class Bill(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bankId: String,
    val groupId: String,
    val description: String,
    val monthlyAmount: Double,
    val totalInstallments: Int,
    val currentInstallment: Int,
    val totalAmount: Double,
    val isPaid: Boolean,
    val dueDay: Int,
    val category: String, // Salvo como String no BD, convertido para Enum na UI
    val notes: String,
    val month: Int,
    val year: Int,
    val timestamp: Long
)

// Relacionamento 1 para N do Room
data class BankWithBills(
    @Embedded val bank: Bank,
    @Relation(
        parentColumn = "id",
        entityColumn = "bankId"
    )
    val bills: List<Bill>
)
```

## 2. O Data Access Object (DAO) e Repository
Aqui acontece a mágica reativa. O `Flow` notifica a UI automaticamente sempre que uma linha na tabela mudar.

```kotlin
@Dao
interface BillsDao {
    @Transaction
    @Query("SELECT * FROM banks WHERE isArchived = 0")
    fun getActiveBanksWithBills(): Flow<List<BankWithBills>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :billId")
    suspend fun updateBillStatus(billId: String, isPaid: Boolean)
}

// Repositório injetado via Hilt
class BillsRepositoryImpl @Inject constructor(
    private val dao: BillsDao
) : BillsRepository {

    override fun getBanksFlow(): Flow<List<BankWithBills>> = dao.getActiveBanksWithBills()

    override suspend fun saveBill(bill: Bill) = withContext(Dispatchers.IO) {
        dao.insertBill(bill)
    }
}
```

## 3. Home UiState e ViewModel
O ViewModel não sabe nada de Android. Ele apenas expõe um estado imutável para o Compose e processa as "Intenções" (Eventos) do usuário.

```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val banks: List<BankWithBills> = emptyList(),
    val currentMonthYear: MonthYear = MonthYear.current(),
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BillsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeDatabase()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            // Combine flows se necessário, mas o repositório já reage a mudanças
            repository.getBanksFlow()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { banksList ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            banks = banksList,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ToggleBillPaid -> toggleBill(intent.billId)
            is HomeIntent.ChangeMonth -> changeMonth(intent.next)
            is HomeIntent.Search -> _uiState.update { it.copy(searchQuery = intent.query) }
            // ...
        }
    }

    private fun toggleBill(billId: String) {
        viewModelScope.launch {
            repository.updateBillStatus(billId)
        }
    }
}
```