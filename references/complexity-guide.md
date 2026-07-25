# Cyclomatic Complexity Guide

## What is Cyclomatic Complexity?

Cyclomatic complexity, developed by Thomas J. McCabe in 1976, is a software metric that measures the number of linearly independent paths through a program's source code. It provides a quantitative measure of the complexity of a program.

### The Formula

**v(G) = E - N + 2P**

Where:
- E = number of edges in the control flow graph
- N = number of nodes in the control flow graph
- P = number of connected components (usually 1 for a single method)

### Simplified Formula

In practice, we use an equivalent formula:

**v(G) = Number of Decision Points + 1**

This counts:
- 1 for the method entry point
- +1 for each decision point (if, for, while, switch case, catch, etc.)

## Decision Points in Java

The analyzer counts the following decision points:

| Decision Point | Description | Example |
|----------------|-------------|---------|
| `if` | Conditional statement | `if (x > 0)` |
| `for` | Loop statement | `for (int i = 0; i < n; i++)` |
| `while` | Loop statement | `while (running)` |
| `do` | Do-while loop | `do { ... } while (cond)` |
| `switch` | Each case counts | `case VALUE:` |
| `catch` | Exception handling | `catch (Exception e)` |
| `&&` | Logical AND | `if (a && b)` |
| `||` | Logical OR | `if (a || b)` |
| `?:` | Ternary operator | `x > 0 ? a : b` |

## Complexity Thresholds

### Industry Standards

| Complexity | Risk | Testing Required | Recommended Action |
|------------|------|------------------|-------------------|
| 1-5 | Low | 1-5 unit tests | No action needed |
| 6-10 | Moderate | 6-10 unit tests | Monitor, consider refactoring |
| 11-20 | High | 11-20 unit tests | Consider refactoring |
| 21-40 | Very High | 21-40 unit tests | Must refactor |
| 41+ | Untestable | - | Immediate refactoring required |

### Recommended Thresholds

- **Strict (Google, NASA)**: 10
- **Moderate (Industry average)**: 15
- **Relaxed (Legacy code)**: 20

## Interpreting Complexity Scores

### Low Complexity (1-5)

**Characteristics:**
- Simple, linear code flow
- Easy to understand at a glance
- Minimal testing required
- Low risk of defects

**Example:**
```java
public int add(int a, int b) {
    return a + b;  // Complexity: 1
}
```

### Moderate Complexity (6-10)

**Characteristics:**
- Contains some conditional logic
- Still manageable to understand
- Moderate testing needed
- Acceptable for most business logic

**Example:**
```java
public String getStatus(int code) {
    if (code == 200) {
        return "OK";
    } else if (code >= 400 && code < 500) {
        return "Client Error";
    } else if (code >= 500) {
        return "Server Error";
    }
    return "Unknown";
}  // Complexity: 4
```

### High Complexity (11-20)

**Characteristics:**
- Multiple nested conditions
- Harder to understand and maintain
- Requires significant testing
- Refactoring recommended

**Warning Signs:**
- Deep nesting (3+ levels)
- Multiple responsibilities
- Complex boolean expressions
- State machine-like logic

### Very High Complexity (21+)

**Characteristics:**
- Difficult to understand
- High maintenance cost
- High defect risk
- Very difficult to test thoroughly

**Immediate Actions Required:**
- Break down into smaller methods
- Extract nested logic
- Apply design patterns
- Consider restructuring

## Refactoring Strategies

### 1. Extract Method

Break large methods into smaller, focused methods.

**Before:**
```java
public void processOrder(Order order) {
    // Validate order (complexity: 3)
    if (order == null) throw new IllegalArgumentException();
    if (!order.isValid()) throw new ValidationException();
    
    // Calculate totals (complexity: 5)
    double total = 0;
    for (Item item : order.getItems()) {
        if (item.isDiscounted()) {
            total += item.getPrice() * 0.9;
        } else {
            total += item.getPrice();
        }
    }
    
    // Save order (complexity: 4)
    // ...
}  // Total complexity: 12+
```

**After:**
```java
public void processOrder(Order order) {
    validateOrder(order);      // Complexity: 3
    double total = calculateTotal(order);  // Complexity: 5
    saveOrder(order, total);   // Complexity: 2
}  // Complexity: 3

private void validateOrder(Order order) { /* ... */ }
private double calculateTotal(Order order) { /* ... */ }
private void saveOrder(Order order, double total) { /* ... */ }
```

### 2. Replace Conditional with Polymorphism

Use object-oriented patterns to eliminate switch statements.

**Before:**
```java
public double calculateShipping(Order order) {
    switch (order.getShippingType()) {
        case STANDARD:
            return order.getWeight() * 1.5;
        case EXPRESS:
            return order.getWeight() * 3.0 + 10;
        case FREE:
            return 0;
        default:
            throw new IllegalArgumentException();
    }
}  // Complexity: 5
```

**After:**
```java
public double calculateShipping(Order order) {
    return order.getShippingStrategy().calculate(order);
}  // Complexity: 1
```

### 3. Decompose Conditional

Extract complex conditions into descriptive methods.

**Before:**
```java
if (user != null && user.isActive() && 
    user.getAge() >= 18 && user.hasPermission("READ")) {
    // ...
}
```

**After:**
```java
if (canAccessResource(user)) {
    // ...
}

private boolean canAccessResource(User user) {
    return user != null && 
           user.isActive() && 
           user.getAge() >= 18 && 
           user.hasPermission("READ");
}
```

### 4. Remove Flag Arguments

Split methods that take boolean parameters.

**Before:**
```java
public void process(boolean saveToDatabase) {
    // ... processing logic
    if (saveToDatabase) {
        save();
    }
}
```

**After:**
```java
public void process() {
    // ... processing logic
}

public void processAndSave() {
    process();
    save();
}
```

### 5. Use Stream API (Java 8+)

Replace loops with functional operations.

**Before:**
```java
List<String> validEmails = new ArrayList<>();
for (User user : users) {
    if (user.isActive()) {
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            validEmails.add(email);
        }
    }
}
```

**After:**
```java
List<String> validEmails = users.stream()
    .filter(User::isActive)
    .map(User::getEmail)
    .filter(email -> email != null && email.contains("@"))
    .collect(Collectors.toList());
```

## Best Practices

### 1. Set Team Standards

- Define complexity thresholds in your project
- Enforce via code review
- Use CI/CD to block high-complexity commits

### 2. Regular Monitoring

- Run complexity analysis as part of build process
- Track complexity trends over time
- Address increases promptly

### 3. Balance Complexity and Readability

Sometimes slightly higher complexity is acceptable if it improves readability:

```java
// Complexity: 3, but clear
if (isValidUser(user) && hasPermission(user, resource)) {
    grantAccess();
}

// vs splitting into smaller methods that obscure logic
```

### 4. Document Complex Logic

When complexity is unavoidable, document thoroughly:

```java
/**
 * Calculates tax based on jurisdiction and product type.
 * 
 * Complexity is 15 due to varying tax rules across 50 states,
 * each with different categories (food, luxury, standard).
 * 
 * Consider breaking down if more jurisdictions are added.
 */
public double calculateTax(Product product, String state) {
    // Implementation...
}
```

## Common Pitfalls

### 1. Over-Engineering

Don't split methods just to reduce complexity if it hurts readability:

```java
// Don't do this
public void processA() { step1(); }
public void processB() { step2(); }
// ... when they're never called separately
```

### 2. Ignoring Context

A complexity of 15 in a data transformation might be fine, 
but in business logic it's a red flag.

### 3. Chasing Perfection

Don't refactor working code with complexity 8 just to get it to 5.
Focus on the high-complexity methods first.

## Tools and Integration

### IDE Integration

Most IDEs provide complexity metrics:
- **IntelliJ IDEA**: Analyze > Calculate Metrics
- **Eclipse**: Code Analysis plugins
- **VS Code**: Extensions available

### Build Integration

```xml
<!-- Maven -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>javancss-maven-plugin</artifactId>
</plugin>
```

### CI/CD Gates

```bash
# Fail build if any method exceeds threshold
python scripts/analyze_complexity.py src/ --recursive --threshold 15
if [ $? -eq 2 ]; then
    echo "Complexity threshold exceeded"
    exit 1
fi
```

## Further Reading

- McCabe, T.J. "A Complexity Measure" (1976)
- Martin, R.C. "Clean Code" (Chapter 3: Functions)
- Fowler, M. "Refactoring" (Chapter 3: Bad Smells in Code)
