#!/usr/bin/env python3
"""
Cyclomatic Complexity Analyzer for Java

Measures McCabe's cyclomatic complexity v(G) for Java source code.
Formula: v(G) = decision points + 1

Decision points counted:
- Control flow: if, for, while, do, switch, catch
- Logical operators: &&, ||
- Ternary operator: ?:
"""

import argparse
import csv
import json
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple


@dataclass
class MethodComplexity:
    """Represents complexity metrics for a single method."""
    name: str
    class_name: str
    package: str
    file_path: str
    line_number: int
    complexity: int
    decision_points: List[str] = field(default_factory=list)


@dataclass
class FileComplexity:
    """Represents complexity metrics for a file."""
    file_path: str
    package: str
    methods: List[MethodComplexity] = field(default_factory=list)

    @property
    def max_complexity(self) -> int:
        if not self.methods:
            return 0
        return max(m.complexity for m in self.methods)

    @property
    def total_complexity(self) -> int:
        return sum(m.complexity for m in self.methods)


class JavaComplexityAnalyzer:
    """Analyzes Java source code to calculate cyclomatic complexity."""

    # Decision point patterns
    CONTROL_FLOW_KEYWORDS = [
        r'\bif\s*\(',
        r'\bfor\s*\(',
        r'\bwhile\s*\(',
        r'\bdo\s*\{',
        r'\bswitch\s*\(',
        r'\bcatch\s*\(',
    ]

    # Logical operators (count each occurrence)
    LOGICAL_OPERATORS = [
        r'&&',
        r'\|\|',
    ]

    # Ternary operator
    TERNARY_PATTERN = r'\?[^:]+:'

    # Method signature pattern
    METHOD_PATTERN = re.compile(
        r'^(?:\s*(?:public|private|protected|static|final|abstract|synchronized|native|strictfp)\s+)*'  # modifiers
        r'(?:<[^>]+>\s+)?'  # generic type parameters
        r'(?:[\w<>,\[\]\.\s]+)\s+'  # return type
        r'(\w+)\s*'  # method name
        r'\([^)]*\)\s*'  # parameters
        r'(?:throws\s+[\w,\s]+)?\s*'  # throws clause
        r'(?:\{|\Z)',  # opening brace or end (for abstract methods)
        re.MULTILINE
    )

    # Class pattern
    CLASS_PATTERN = re.compile(
        r'^(?:\s*(?:public|private|protected|static|final|abstract)\s+)*'
        r'(?:class|interface|enum|record)\s+'
        r'(\w+)',
        re.MULTILINE
    )

    # Package pattern
    PACKAGE_PATTERN = re.compile(r'package\s+([\w.]+)\s*;')

    def __init__(self, threshold: int = 10):
        self.threshold = threshold
        self.control_flow_regex = [re.compile(p) for p in self.CONTROL_FLOW_KEYWORDS]
        self.logical_op_regex = [re.compile(p) for p in self.LOGICAL_OPERATORS]
        self.ternary_regex = re.compile(self.TERNARY_PATTERN)

    def _remove_comments_and_strings(self, content: str) -> str:
        """Remove comments and string literals from code for accurate parsing."""
        result = []
        i = 0
        length = len(content)

        while i < length:
            # Handle string literals
            if content[i] == '"':
                # Check for escaped quote
                if i > 0 and content[i-1] == '\\':
                    result.append(content[i])
                    i += 1
                    continue
                result.append('"')
                i += 1
                while i < length:
                    if content[i] == '"' and content[i-1] != '\\':
                        result.append('"')
                        i += 1
                        break
                    elif content[i] == '\\':
                        result.append('\\')
                        i += 1
                    else:
                        result.append(' ')
                        i += 1
                continue

            # Handle character literals
            if content[i] == "'":
                result.append("'")
                i += 1
                while i < length and content[i] != "'":
                    if content[i] == '\\':
                        result.append('\\')
                        i += 1
                        if i < length:
                            result.append(content[i])
                            i += 1
                    else:
                        result.append(' ')
                        i += 1
                if i < length:
                    result.append("'")
                    i += 1
                continue

            # Handle single-line comments
            if content[i:i+2] == '//':
                while i < length and content[i] != '\n':
                    result.append(' ')
                    i += 1
                continue

            # Handle multi-line comments
            if content[i:i+2] == '/*':
                while i < length:
                    if content[i:i+2] == '*/':
                        result.append('  ')
                        i += 2
                        break
                    if content[i] == '\n':
                        result.append('\n')
                    else:
                        result.append(' ')
                    i += 1
                continue

            result.append(content[i])
            i += 1

        return ''.join(result)

    def _count_decision_points(self, code_block: str) -> Tuple[int, List[str]]:
        """Count decision points in a code block."""
        decision_points = []
        count = 0

        # Count control flow keywords
        for pattern, name in zip(self.control_flow_regex, 
                                  ['if', 'for', 'while', 'do', 'switch', 'catch']):
            matches = pattern.findall(code_block)
            count += len(matches)
            for _ in matches:
                decision_points.append(name)

        # Count logical operators
        for pattern, name in zip(self.logical_op_regex, ['&&', '||']):
            matches = pattern.findall(code_block)
            count += len(matches)
            for _ in matches:
                decision_points.append(name)

        # Count ternary operators
        ternary_matches = self.ternary_regex.findall(code_block)
        count += len(ternary_matches)
        for _ in ternary_matches:
            decision_points.append('?:')

        # Complexity = decision points + 1
        return count + 1, decision_points

    def _find_method_boundaries(self, content: str) -> List[Tuple[int, int, int]]:
        """Find start line, end line, and body start for each method."""
        methods = []
        lines = content.split('\n')

        for match in self.METHOD_PATTERN.finditer(content):
            method_start = match.start()
            line_number = content[:method_start].count('\n') + 1

            # Find the opening brace position
            brace_pos = match.end() - 1
            if content[brace_pos] == '{':
                brace_pos += 1
            else:
                # Abstract method or interface method without body
                continue

            # Find matching closing brace
            brace_count = 1
            pos = brace_pos
            while pos < len(content) and brace_count > 0:
                if content[pos] == '{':
                    brace_count += 1
                elif content[pos] == '}':
                    brace_count -= 1
                pos += 1

            method_end_line = content[:pos].count('\n') + 1
            methods.append((line_number, method_end_line, brace_pos, pos))

        return methods

    def analyze_file(self, file_path: str) -> Optional[FileComplexity]:
        """Analyze a single Java file for cyclomatic complexity."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except Exception as e:
            print(f"Error reading file {file_path}: {e}", file=sys.stderr)
            return None

        # Extract package
        package_match = self.PACKAGE_PATTERN.search(content)
        package = package_match.group(1) if package_match else 'default'

        # Find all classes
        classes = self.CLASS_PATTERN.findall(content)
        current_class = classes[0] if classes else 'Unknown'

        # Clean content for analysis
        clean_content = self._remove_comments_and_strings(content)

        file_complexity = FileComplexity(
            file_path=file_path,
            package=package
        )

        # Find method boundaries
        method_boundaries = self._find_method_boundaries(content)

        for match in self.METHOD_PATTERN.finditer(content):
            method_name = match.group(1)

            # Find which class this method belongs to (simplified)
            method_pos = match.start()
            for cls_match in self.CLASS_PATTERN.finditer(content):
                if cls_match.start() < method_pos:
                    current_class = cls_match.group(1)

            line_number = content[:match.start()].count('\n') + 1

            # Find method body boundaries
            brace_pos = match.end() - 1
            if brace_pos >= len(content) or content[brace_pos] != '{':
                continue

            # Find matching closing brace
            brace_count = 1
            pos = brace_pos + 1
            while pos < len(clean_content) and brace_count > 0:
                if clean_content[pos] == '{':
                    brace_count += 1
                elif clean_content[pos] == '}':
                    brace_count -= 1
                pos += 1

            method_body = clean_content[brace_pos:pos]
            complexity, decision_points = self._count_decision_points(method_body)

            method_complexity = MethodComplexity(
                name=method_name,
                class_name=current_class,
                package=package,
                file_path=file_path,
                line_number=line_number,
                complexity=complexity,
                decision_points=decision_points
            )
            file_complexity.methods.append(method_complexity)

        return file_complexity

    def analyze_directory(self, directory: str, recursive: bool = False) -> List[FileComplexity]:
        """Analyze all Java files in a directory."""
        results = []

        if recursive:
            pattern = '**/*.java'
        else:
            pattern = '*.java'

        path = Path(directory)
        for java_file in path.glob(pattern):
            if java_file.is_file():
                result = self.analyze_file(str(java_file))
                if result:
                    results.append(result)

        return results


def get_risk_level(complexity: int) -> Tuple[str, str]:
    """Get risk level and indicator for a complexity value."""
    if complexity <= 5:
        return 'Low', '✓'
    elif complexity <= 10:
        return 'Moderate', '○'
    elif complexity <= 20:
        return 'High', '●'
    else:
        return 'Very High', '⚠'


def format_text_output(results: List[FileComplexity], threshold: int, 
                       summary_only: bool = False, verbose: bool = False) -> str:
    """Format results as human-readable text."""
    lines = []
    lines.append("=" * 70)
    lines.append("Cyclomatic Complexity Analysis Report")
    lines.append("=" * 70)
    lines.append("")

    all_methods = []
    for file_result in results:
        all_methods.extend(file_result.methods)

    if not all_methods:
        lines.append("No methods found in analyzed files.")
        return '\n'.join(lines)

    # Summary statistics
    complexities = [m.complexity for m in all_methods]
    avg_complexity = sum(complexities) / len(complexities)
    max_complexity = max(complexities)
    methods_over_threshold = sum(1 for c in complexities if c > threshold)

    lines.append(f"Files analyzed: {len(results)}")
    lines.append(f"Methods analyzed: {len(all_methods)}")
    lines.append(f"Average complexity: {avg_complexity:.2f}")
    lines.append(f"Maximum complexity: {max_complexity}")
    lines.append(f"Methods over threshold ({threshold}): {methods_over_threshold}")
    lines.append("")

    if summary_only:
        return '\n'.join(lines)

    # Detailed method listing
    lines.append("-" * 70)
    lines.append("Method Details")
    lines.append("-" * 70)
    lines.append("")

    # Sort by complexity descending
    sorted_methods = sorted(all_methods, key=lambda m: m.complexity, reverse=True)

    for method in sorted_methods:
        risk, indicator = get_risk_level(method.complexity)
        warning = " [OVER THRESHOLD]" if method.complexity > threshold else ""

        lines.append(f"{indicator} {method.class_name}.{method.name}() - "
                    f"Complexity: {method.complexity} ({risk}){warning}")
        lines.append(f"   File: {method.file_path}:{method.line_number}")

        if verbose and method.decision_points:
            # Count occurrences of each decision point type
            point_counts = {}
            for point in method.decision_points:
                point_counts[point] = point_counts.get(point, 0) + 1

            points_str = ', '.join(f"{k}({v})" for k, v in sorted(point_counts.items()))
            lines.append(f"   Decision points: {points_str}")

        lines.append("")

    # Risk level legend
    lines.append("-" * 70)
    lines.append("Risk Levels")
    lines.append("-" * 70)
    lines.append("  ✓ 1-5    : Low - No action needed")
    lines.append("  ○ 6-10   : Moderate - Monitor")
    lines.append("  ● 11-20  : High - Consider refactoring")
    lines.append("  ⚠ 21+    : Very High - Must refactor")
    lines.append("")

    return '\n'.join(lines)


def format_json_output(results: List[FileComplexity], threshold: int) -> str:
    """Format results as JSON."""
    output = {
        "summary": {
            "files_analyzed": len(results),
            "total_methods": sum(len(f.methods) for f in results),
            "threshold": threshold
        },
        "files": []
    }

    all_complexities = []
    for file_result in results:
        file_data = {
            "file_path": file_result.file_path,
            "package": file_result.package,
            "method_count": len(file_result.methods),
            "max_complexity": file_result.max_complexity,
            "methods": []
        }

        for method in file_result.methods:
            all_complexities.append(method.complexity)
            risk, _ = get_risk_level(method.complexity)

            method_data = {
                "name": method.name,
                "class_name": method.class_name,
                "line_number": method.line_number,
                "complexity": method.complexity,
                "risk_level": risk,
                "over_threshold": method.complexity > threshold,
                "decision_points": method.decision_points
            }
            file_data["methods"].append(method_data)

        output["files"].append(file_data)

    if all_complexities:
        output["summary"]["average_complexity"] = sum(all_complexities) / len(all_complexities)
        output["summary"]["max_complexity"] = max(all_complexities)
        output["summary"]["methods_over_threshold"] = sum(1 for c in all_complexities if c > threshold)

    return json.dumps(output, indent=2)


def format_csv_output(results: List[FileComplexity], threshold: int) -> str:
    """Format results as CSV."""
    import io

    output = io.StringIO()
    writer = csv.writer(output)

    # Header
    writer.writerow([
        'File Path', 'Package', 'Class', 'Method', 'Line Number',
        'Complexity', 'Risk Level', 'Over Threshold'
    ])

    for file_result in results:
        for method in file_result.methods:
            risk, _ = get_risk_level(method.complexity)
            writer.writerow([
                method.file_path,
                method.package,
                method.class_name,
                method.name,
                method.line_number,
                method.complexity,
                risk,
                'Yes' if method.complexity > threshold else 'No'
            ])

    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(
        description='Analyze cyclomatic complexity of Java source code'
    )
    parser.add_argument(
        'path',
        help='Path to Java file or directory to analyze'
    )
    parser.add_argument(
        '--threshold', '-t',
        type=int,
        default=10,
        help='Complexity threshold for warnings (default: 10)'
    )
    parser.add_argument(
        '--recursive', '-r',
        action='store_true',
        help='Scan directories recursively'
    )
    parser.add_argument(
        '--format', '-f',
        choices=['text', 'json', 'csv'],
        default='text',
        help='Output format (default: text)'
    )
    parser.add_argument(
        '--summary', '-s',
        action='store_true',
        help='Show only summary statistics'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Show decision point details'
    )

    args = parser.parse_args()

    # Validate path
    if not os.path.exists(args.path):
        print(f"Error: Path not found: {args.path}", file=sys.stderr)
        sys.exit(1)

    analyzer = JavaComplexityAnalyzer(threshold=args.threshold)

    # Analyze file or directory
    if os.path.isfile(args.path):
        if not args.path.endswith('.java'):
            print(f"Error: Not a Java file: {args.path}", file=sys.stderr)
            sys.exit(1)
        results = [analyzer.analyze_file(args.path)]
        if results[0] is None:
            sys.exit(1)
    else:
        results = analyzer.analyze_directory(args.path, recursive=args.recursive)

    if not results:
        print("No Java files found to analyze.", file=sys.stderr)
        sys.exit(1)

    # Generate output
    if args.format == 'json':
        print(format_json_output(results, args.threshold))
    elif args.format == 'csv':
        print(format_csv_output(results, args.threshold))
    else:
        print(format_text_output(results, args.threshold, args.summary, args.verbose))

    # Determine exit code
    all_methods = []
    for r in results:
        all_methods.extend(r.methods)

    methods_over_threshold = sum(1 for m in all_methods if m.complexity > args.threshold)

    if methods_over_threshold > 0:
        sys.exit(2)
    else:
        sys.exit(0)


if __name__ == '__main__':
    main()
